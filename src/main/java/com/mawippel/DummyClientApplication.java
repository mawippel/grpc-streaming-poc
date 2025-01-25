package com.mawippel;

import com.mawippel.constants.HeaderInterceptorConstants;
import com.poc.proto.ExchangeRequest;
import com.poc.proto.ExchangeResponse;
import com.poc.proto.GrpcStreamServiceGrpc;
import com.poc.proto.MessageType;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DummyClientApplication {

    private static StreamObserver<ExchangeRequest> request;

    public static void main(String[] args) throws IOException {
        /*
        File certChain = new ClassPathResource("certs/sample-client.crt").getFile();
        File privateKey = new ClassPathResource("certs/sample-client.key").getFile();
        File certificateAuthority = new ClassPathResource("certs/rootCA.crt").getFile();

        ChannelCredentials channelCredentials = TlsChannelCredentials.newBuilder()
                .keyManager(certChain, privateKey)
                .trustManager(certificateAuthority)
                .build();
         */

        var managedChannel = NettyChannelBuilder
                // .forAddress("localhost", 9090, channelCredentials)
                .forAddress("localhost", 9090)
                .keepAliveTime(120, TimeUnit.SECONDS)
                .keepAliveTimeout(1, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();

        Metadata meta = new Metadata();
        //meta.put(HeaderInterceptorConstants.DEVICE_ID_HEADER_KEY, "Z0VOHUVS9ERLZ1AC");
        meta.put(HeaderInterceptorConstants.SCOPE_HEADER_KEY, "ALERTS");
        meta.put(HeaderInterceptorConstants.MODEL_HEADER_KEY, "WT6300");
        meta.put(HeaderInterceptorConstants.UDI_HEADER_KEY, "WT6300_123123123123");

        request = GrpcStreamServiceGrpc.newStub(managedChannel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(meta))
                .exchange(new StreamObserver<>() {
                    @Override
                    public void onNext(ExchangeResponse value) {
                        log.info("received data from the server: " + value.getData());
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        System.out.println("received ERROR: " + t);
                    }

                    @Override
                    public void onCompleted() {
                        log.info("server completed the request (disconnected)");
                    }
                });

        while (true) {
            try {
                // this initial message will AutoClaim the device if it doesn't exist
                request.onNext(ExchangeRequest.newBuilder()
                        .setData("{\"autoClaimMsg\":\"hello\"}")
                        .setMessageType(MessageType.DEVICE_DATA)
                        .build());
                Thread.sleep(Duration.of(60, ChronoUnit.SECONDS).toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
                request.onError(e);
                managedChannel.shutdown();
            }
        }
    }
}
