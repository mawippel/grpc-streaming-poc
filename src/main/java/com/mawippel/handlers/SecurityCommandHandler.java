package com.mawippel.handlers;

import com.mawippel.server.GrpcStreamServiceGRPCServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityCommandHandler {
    private final ApplicationContext appCtx;

    public SecurityCommandHandler(ApplicationContext appCtx) {
        this.appCtx = appCtx;
    }

    public void handleCertUpdate() {
        log.info("Restarting gRPC server with the latest cert update.");
        GrpcStreamServiceGRPCServer gRPCServer = appCtx.getBean(GrpcStreamServiceGRPCServer.class);
        gRPCServer.restart();
    }

}
