package com.mawippel;

import com.mawippel.server.GrpcStreamServiceGRPCServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@EnableScheduling
@Slf4j
@SpringBootApplication
public class GrpcStreamServiceApplication {

    public static void main(String[] args) throws InterruptedException, IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        var podName = System.getenv("POD_NAME");
        if (podName == null) {
            var rnd = new Random();
            podName = "local-" + rnd.nextInt(0, Integer.MAX_VALUE);
        }
        System.setProperty("POD_NAME", podName);

        var app = new SpringApplicationBuilder(GrpcStreamServiceApplication.class).build();
        LocalDateTime start = LocalDateTime.now();
        var appCtx = app.run(args);
        var buildProps = appCtx.getBean(BuildProperties.class);
        LocalDateTime end = LocalDateTime.now();
        String applicationStartTime = Duration.between(start, end).toString().substring(2).toLowerCase();
        log.warn("application {} version {} successfully started in {}", buildProps.getName(), buildProps.getVersion(), applicationStartTime);

        GrpcStreamServiceGRPCServer gRPCServer = appCtx.getBean(GrpcStreamServiceGRPCServer.class);
        gRPCServer.startInternalServer();
        gRPCServer.startExternalServer();
        gRPCServer.blockUntilShutdownInternalServer();
        gRPCServer.blockUntilShutdownExternalServer();
    }

}
