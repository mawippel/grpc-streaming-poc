package com.mawippel.resources;

import com.google.protobuf.Empty;
import com.mawippel.constants.HeaderInterceptorConstants;
import com.mawippel.domain.DeviceMessage;
import com.mawippel.interceptors.RemoteAddressInterceptor;
import com.mawippel.interceptors.SSLContextInterceptor;
import com.mawippel.services.GrpcStreamService;
import com.poc.proto.*;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

@Slf4j
@Controller
public class GrpcStreamResource extends GrpcStreamServiceGrpc.GrpcStreamServiceImplBase {

    private final Map<String, StreamObserver<ExchangeResponse>> connections;
    private final String podName;
    private final GrpcStreamService grpcStreamService;

    public GrpcStreamResource(@Value("${k8s.metadata.name}") String podName,
                              GrpcStreamService grpcStreamService,
                              Map<String, StreamObserver<ExchangeResponse>> connections) {
        this.podName = podName;
        this.grpcStreamService = grpcStreamService;
        this.connections = connections;
    }


    @Override
    public StreamObserver<ExchangeRequest> exchange(StreamObserver<ExchangeResponse> responseObserver) {
        // get the Certificate Name from the SSL Context
        try {
            String certificateName;
            SSLSession sslSession = SSLContextInterceptor.SSL_SESSION_CONTEXT.get();
            if (sslSession != null) {
                // this variable will be used in the upcoming changes
                certificateName = sslSession.getPeerPrincipal().getName();
                log.debug(certificateName);
            }
        } catch (SSLPeerUnverifiedException e) {
            responseObserver.onError(e);
            return null;
        }

        String ipAddress = getClientIpAddress();

        Map<String, String> headers = HeaderInterceptorConstants.HEADERS_CONTEXT.get();
        String deviceIdFromHeader = headers.get(HeaderInterceptorConstants.DEVICE_ID);
        String model = headers.get(HeaderInterceptorConstants.MODEL);
        String udiFromHeader = headers.get(HeaderInterceptorConstants.UDI);
        String scope = headers.get(HeaderInterceptorConstants.SCOPE);

        try {
            validateRequiredHeaders(deviceIdFromHeader, model, udiFromHeader, scope);
        } catch (StatusException e) {
            responseObserver.onError(e);
            return null;
        }

        /*
            We need both the UDI and deviceId, so:
             - If the udi from the header is empty, fetch the udi using the deviceIdFromHeader;
             - If the deviceId from the header is empty, fetch the deviceId using the UDI;
         */
        final String udi;
        final String deviceId;
        try {
            udi = grpcStreamService.getUdiFromDeviceIdIfEmpty(udiFromHeader, deviceIdFromHeader);
            deviceId = grpcStreamService.getDeviceIdFromUDI(deviceIdFromHeader, model, udiFromHeader);
        } catch (StatusException e) {
            responseObserver.onError(e);
            return null;
        }

        // Check if this device is already connected to this pod
        StreamObserver<ExchangeResponse> removedConnection = disconnectDeviceFromConnectionMap(udi);
        if (removedConnection == null) {
            // Check if the device is connected to another instance and disconnect it
            grpcStreamService.disconnectFromAnotherPodIfConnected(udi);
        }

        connections.put(udi, responseObserver);
        grpcStreamService.addDeviceToConnectionMap(deviceId, udi, ipAddress);

        return new StreamObserver<>() {
            @Override
            public void onNext(ExchangeRequest value) {
                log.debug("message '{}' from '{}'", value.getData(), udi);
                grpcStreamService.handleMessage(DeviceMessage.builder()
                        .deviceId(deviceId)
                        .udi(udi)
                        .ipAddress(ipAddress)
                        .scope(scope)
                        .message(value.getData())
                        .messageType(value.getMessageType())
                        .build()
                );
            }

            @Override
            public void onError(Throwable t) {
                log.error(udi, t);
                removeDeviceFromConnectionMap();
            }

            @Override
            public void onCompleted() {
                log.debug("device {} completed the request", udi);
                removeDeviceFromConnectionMap();
            }

            /**
             * Removes the device from the connection map and the Redis cache.
             */
            private void removeDeviceFromConnectionMap() {
                connections.remove(udi);
                grpcStreamService.removeDeviceFromConnectionMap(deviceId, udi);
            }
        };
    }

    private String getClientIpAddress() {
        String ipAddress = "unknown";
        SocketAddress socketAddress = RemoteAddressInterceptor.REMOTE_ADDR_CONTEXT.get();
        if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
            ipAddress = inetSocketAddress.getHostString();
        }
        return ipAddress;
    }

    public void sendMessage(SendMessageRequest request, StreamObserver<Empty> responseObserver) {
        log.debug("Sending message to [deviceIdentifier={}]", request.getDeviceIdentifier());
        String deviceIdentifier = request.getDeviceIdentifier();
        StreamObserver<ExchangeResponse> deviceStreamObserver = connections.get(deviceIdentifier);
        if (deviceStreamObserver == null) {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND
                    .withDescription("[deviceIdentifier=%s] is not connected to the pod.".formatted(deviceIdentifier))));
            return;
        }

        try {
            deviceStreamObserver.onNext(ExchangeResponse.newBuilder()
                    .setMessageType(MessageType.DEVICE_ACTION)
                    .setData(request.getMessagePayload())
                    .setTag(request.getTag())
                    .build());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            log.error("Error sending message downstream to [deviceIdentifier=%s], disconnecting the device.".formatted(deviceIdentifier), e);
            connections.remove(deviceIdentifier);
            grpcStreamService.removeDeviceFromConnectionMap("", deviceIdentifier);

            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription("Error sending message downstream to [deviceIdentifier=%s].".formatted(deviceIdentifier))));
        }
    }

    public void disconnectDevice(DisconnectDeviceRequest request, StreamObserver<Empty> responseObserver) {
        String deviceIdentifier = request.getDeviceIdentifier();
        log.debug("Disconnecting device [deviceIdentifier={}]", deviceIdentifier);
        disconnectDeviceFromConnectionMap(request.getDeviceIdentifier());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    private StreamObserver<ExchangeResponse> disconnectDeviceFromConnectionMap(String udi) {
        StreamObserver<ExchangeResponse> removedConnection = connections.remove(udi);
        if (removedConnection != null) {
            removedConnection.onError(new StatusException(Status.CANCELLED
                    .withDescription("Connection cancelled by the server. A new connection was made using the same device identifier."))
            );
            log.info("deviceIdentifier={} connection cancelled by the server.", udi);
        }
        return removedConnection;
    }

    private void validateRequiredHeaders(String deviceId, String model, String udi, String scope) throws StatusException {
        if (StringUtils.isBlank(scope)) {
            throw new StatusException(Status.INVALID_ARGUMENT.withDescription("Required header 'scope' is not present."));
        }

        if (StringUtils.isBlank(deviceId)
                && (StringUtils.isBlank(model) || StringUtils.isBlank(udi))) {
            throw new StatusException(Status.INVALID_ARGUMENT
                    .withDescription("Required headers to identity the device are not present. Either 'deviceId' or 'model' + 'udi' must be present."));
        }
    }

}
