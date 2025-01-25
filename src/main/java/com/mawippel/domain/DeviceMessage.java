package com.mawippel.domain;

import com.poc.proto.MessageType;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DeviceMessage {

    private String deviceId;
    private String udi;
    private String ipAddress;
    private String scope;
    private String message;
    private MessageType messageType;

}
