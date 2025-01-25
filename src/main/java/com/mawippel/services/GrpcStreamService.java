package com.mawippel.services;

import com.google.common.collect.Maps;
import com.mawippel.domain.DeviceMessage;
import com.mawippel.services.handlers.DeviceActionResponseHandler;
import com.mawippel.services.handlers.DeviceEventHandler;
import com.mawippel.services.handlers.DeviceMessageHandler;
import com.mawippel.utils.KubernetesUtils;
import com.poc.proto.DisconnectDeviceRequest;
import com.poc.proto.InternalGrpcStreamServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class GrpcStreamService {

    private final int grpcStreamServiceInternalPort;
    private final DeviceActionResponseHandler deviceActionResponseHandler;
    private final DeviceEventHandler deviceEventHandler;
    private final RedisAsyncCommands<String, String> redisAsyncCommands;
    private final String podHostname;

    // Keep a "cache" of connections to other pods to not open a new channel for every call
    private final ConcurrentMap<String, ManagedChannel> channelsHashMap = Maps.newConcurrentMap();

    private final Map<String, String> redisCache = new HashMap<>();

    public GrpcStreamService(@Value("${k8s.metadata.namespace}") String k8sNamespace,
                             @Value("${k8s.metadata.ip}") String k8sPodIp,
                             @Value("${grpc.server.internal-port}") int grpcStreamServiceInternalPort,
                             DeviceActionResponseHandler deviceActionResponseHandler,
                             DeviceEventHandler deviceEventHandler,
                             RedisAsyncCommands<String, String> redisAsyncCommands) {
        String podIdInK8sPattern = KubernetesUtils.getIpAddressInKubernetesPattern(k8sPodIp);
        this.podHostname = KubernetesUtils.buildDNSEntry(k8sNamespace, podIdInK8sPattern);
        this.grpcStreamServiceInternalPort = grpcStreamServiceInternalPort;
        this.deviceActionResponseHandler = deviceActionResponseHandler;
        this.deviceEventHandler = deviceEventHandler;
        this.redisAsyncCommands = redisAsyncCommands;
    }

    /**
     * This method handles the message according to the messageType.
     * It also executes side effects for each message (backup, fin ops, etc.)
     */
    public void handleMessage(DeviceMessage deviceMessage) {
        RedisFuture<Long> incrFuture = incrementFinOpsCountAsync(deviceMessage.getScope());
        buildMessageHandler(deviceMessage).handle(deviceMessage);
        try {
            Long currentValue = incrFuture.get();
            log.info("Current FinOps value scope: {} value: {}", deviceMessage.getScope(), currentValue);
        } catch (Exception e) {
            log.error("Error while executing FinOps increment in Redis.", e);
        }
    }

    private DeviceMessageHandler buildMessageHandler(DeviceMessage deviceMessage) {
        return switch (deviceMessage.getMessageType()) {
            case DEVICE_ACTION_RESPONSE -> deviceActionResponseHandler;
            case DEVICE_DATA -> deviceEventHandler;
            default ->
                    throw new RuntimeException("unsupported message type %s".formatted(deviceMessage.getMessageType()));
        };
    }

    public String getUdiFromDeviceIdIfEmpty(String udi, String deviceId) throws StatusException {
        return "sampleUniqueDeviceIdentifier";
    }

    public String getDeviceIdFromUDI(String deviceId, String model, String udi) {
        return "sampleDeviceId";
    }

    /**
     * Adds a new device to the Redis connection map
     */
    public void addDeviceToConnectionMap(String deviceId, String udi, String ipAddress) {
        // TODO add this device to redis
        redisCache.put(udi, ipAddress);
        log.info("deviceId={} deviceIdentifier={} IP={} connected to the pod {}", deviceId, udi, ipAddress, podHostname);
    }

    public void removeDeviceFromConnectionMap(String deviceId, String udi) {
        // removes this device from redis
    }

    public void disconnectFromAnotherPodIfConnected(String udi) {
        String optHostname = redisCache.get(udi);
        ManagedChannel channel = buildChannelForHost(optHostname);
        try {
                /*
                    This is not an async connection because the current connection has to be killed
                    before establishing the new one.
                 */
            InternalGrpcStreamServiceGrpc.newBlockingStub(channel)
                    .disconnectDevice(DisconnectDeviceRequest.newBuilder()
                            .setDeviceIdentifier(udi)
                            .build());
        } catch (Exception e) {
            log.error("error disconnecting deviceIdentifier=%s from another pod".formatted(udi), e);
        }
    }

    private ManagedChannel buildChannelForHost(String host) {
        return channelsHashMap.computeIfAbsent(host, k -> ManagedChannelBuilder
                .forAddress(host, grpcStreamServiceInternalPort)
                .usePlaintext()
                .build()
        );
    }

    /**
     * This method increments the FinOps count in Redis for a particular device scope
     */
    private RedisFuture<Long> incrementFinOpsCountAsync(String deviceScope) {
        return redisAsyncCommands.incr(deviceScope);
    }

}
