package com.mawippel.server;

import com.mawippel.interceptors.GrpcHeaderInterceptor;
import com.mawippel.interceptors.RemoteAddressInterceptor;
import com.mawippel.interceptors.SSLContextInterceptor;
import com.mawippel.resources.GrpcStreamResource;
import com.mawippel.resources.InternalGrpcStreamResource;
import com.mawippel.services.CertificateManagerService;
import io.grpc.Server;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptors;
import io.grpc.TlsServerCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerSocketChannel;
import io.grpc.protobuf.services.HealthStatusManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.grpc.health.v1.HealthCheckResponse.ServingStatus.NOT_SERVING;
import static io.grpc.health.v1.HealthCheckResponse.ServingStatus.SERVING;

@Component
@Slf4j
public class GrpcStreamServiceGRPCServer {

    // see AppConfig.java
    private final Executor grpcExecutor;
    private final int port;
    private int internalPort;
    private final int permitKeepAliveTime;
    private final int keepAliveTime;
    private final int keepAliveTimeout;
    private final GrpcStreamResource grpcStreamResource;
    private final InternalGrpcStreamResource internalGrpcStreamResource;
    private final CertificateManagerService certificateManagerService;
    private final HealthStatusManager healthStatusManager;
    private Server externalServer;
    private Server internalServer;

    public GrpcStreamServiceGRPCServer(Executor grpcExecutor,
                                       GrpcStreamResource grpcStreamResource,
                                       InternalGrpcStreamResource internalGrpcStreamResource,
                                       CertificateManagerService certificateManagerService,
                                       @Value("${grpc.server.port}") int port,
                                       @Value("${grpc.server.internal-port}") int internalPort,
                                       @Value("${grpc.server.permit-keep-alive-time}") int permitKeepAliveTime,
                                       @Value("${grpc.server.keep-alive-time}") int keepAliveTime,
                                       @Value("${grpc.server.keep-alive-timeout}") int keepAliveTimeout
    ) {
        this.grpcExecutor = grpcExecutor;
        this.grpcStreamResource = grpcStreamResource;
        this.certificateManagerService = certificateManagerService;
        this.port = port;
        this.internalPort = internalPort;
        this.permitKeepAliveTime = permitKeepAliveTime;
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeout = keepAliveTimeout;
        this.internalGrpcStreamResource = internalGrpcStreamResource;
        this.healthStatusManager = new HealthStatusManager();
    }

    public void startExternalServer() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        try (var certChain = certificateManagerService.getCertChain().openStream();
             var privateKey = certificateManagerService.getPrivateKey().openStream();
             var rootCerts = certificateManagerService.getRootCerts().openStream()) {

            certificateManagerService.createServerCertChainInGCSIfNotExists(rootCerts);

            startMTLSServer(certChain, privateKey, certificateManagerService.getTrustManager());
        }
    }

    public void restart() {
        try (var certChain = certificateManagerService.getCertChain().openStream();
             var privateKey = certificateManagerService.getPrivateKey().openStream()) {
            // waits 30 seconds (max) to stop the gRPC server
            stopExternalServer();
            log.info("Successfully stopped the gRPC server.");
            startMTLSServer(certChain, privateKey, certificateManagerService.getTrustManager());
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            log.error("Certificate-related error", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.error("Error restarting the gRPC server", e);
            throw new RuntimeException(e);
        }
    }

    private void startMTLSServer(InputStream certChain, InputStream privateKey, TrustManager rootCerts) throws IOException {
        ServerCredentials serverCredentials = TlsServerCredentials.newBuilder()
                .keyManager(certChain, privateKey)
                .trustManager(rootCerts)
                .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                .build();

        externalServer = NettyServerBuilder.forPort(port, serverCredentials)
                .executor(grpcExecutor)
                .addService(ServerInterceptors.intercept(grpcStreamResource,
                        new GrpcHeaderInterceptor(),
                        new SSLContextInterceptor(),
                        new RemoteAddressInterceptor()
                ))
                // recommended here https://stackoverflow.com/a/55619399/11211119
                // thread pool responsible for accepting new connections
                .bossEventLoopGroup(new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors()))
                // thread pool responsible for I/O
                .workerEventLoopGroup(new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2))
                .channelType(EpollServerSocketChannel.class)
                // allows keepalive pings to be sent even if there are no calls in flight
                .permitKeepAliveWithoutCalls(true)
                // Specify the most aggressive keep-alive time clients are permitted to configure
                .permitKeepAliveTime(permitKeepAliveTime, TimeUnit.SECONDS)
                // controls the period after which a keepalive ping is sent on the transport
                .keepAliveTime(keepAliveTime, TimeUnit.SECONDS)
                // the amount of time the sender of the keepalive ping waits for an acknowledgement
                // if it does not receive an acknowledgement within this time, it will close the connection
                .keepAliveTimeout(keepAliveTimeout, TimeUnit.SECONDS)
                .build()
                .start();

        log.info("external gRPC server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down external gRPC server since JVM is shutting down ***");
            try {
                GrpcStreamServiceGRPCServer.this.stopExternalServer();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** gRPC external server shutdown ***");
        }));
    }

    public void startInternalServer() throws IOException {
        internalServer = NettyServerBuilder.forPort(internalPort)
                .addService(healthStatusManager.getHealthService())
                .addService(internalGrpcStreamResource)
                .build()
                .start();
        log.info("internal gRPC server started, listening on " + internalPort);
        healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, SERVING);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down internal gRPC server since JVM is shutting down ***");
            try {
                GrpcStreamServiceGRPCServer.this.stopInternalServer();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** gRPC internal server shutdown ***");
        }));
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdownExternalServer() throws InterruptedException {
        if (externalServer != null) {
            externalServer.awaitTermination();
        }
    }

    private void stopExternalServer() throws InterruptedException {
        if (externalServer != null) {
            externalServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdownInternalServer() throws InterruptedException {
        if (internalServer != null) {
            internalServer.awaitTermination();
        }
    }

    private void stopInternalServer() throws InterruptedException {
        if (internalServer != null) {
            healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, NOT_SERVING);
            internalServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

}
