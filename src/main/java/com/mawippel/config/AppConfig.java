package com.mawippel.config;

import com.google.common.collect.Maps;
import com.mawippel.resources.GrpcStreamResource;
import com.mawippel.server.ServerForkJoinWorkerThreadFactory;
import com.poc.proto.ExchangeResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@Configuration
@Slf4j
public class AppConfig {

    @Bean
    Executor grpcExecutor() {
        var numOfAvailableProcessors = Runtime.getRuntime().availableProcessors();
        log.info("Number of available processors: {}", numOfAvailableProcessors);
        ForkJoinPool.ForkJoinWorkerThreadFactory forkJoinWorkerThreadFactory = ServerForkJoinWorkerThreadFactory::new;
        return new ForkJoinPool(Runtime.getRuntime().availableProcessors(), forkJoinWorkerThreadFactory, null, true);
    }

    /**
     * The type of map used to store the connections in {@link GrpcStreamResource}
     */
    @Bean
    public Map<String, StreamObserver<ExchangeResponse>> getConnectionsMap() {
        return Maps.newConcurrentMap();
    }

}
