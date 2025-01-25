package com.mawippel.resources;

import com.google.protobuf.Empty;
import com.poc.proto.DisconnectDeviceRequest;
import com.poc.proto.InternalGrpcStreamServiceGrpc;
import com.poc.proto.SendMessageRequest;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class InternalGrpcStreamResource extends InternalGrpcStreamServiceGrpc.InternalGrpcStreamServiceImplBase {


    private GrpcStreamResource grpcStreamResource;

    public InternalGrpcStreamResource(GrpcStreamResource grpcStreamResource) {
        this.grpcStreamResource = grpcStreamResource;
    }

    @Override
    public void sendMessage(SendMessageRequest request, StreamObserver<Empty> responseObserver) {
        grpcStreamResource.sendMessage(request, responseObserver);
    }

    @Override
    public void disconnectDevice(DisconnectDeviceRequest request, StreamObserver<Empty> responseObserver) {
        grpcStreamResource.disconnectDevice(request, responseObserver);
    }
}
