package com.mawippel.services.handlers;

import com.mawippel.domain.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeviceEventHandler implements DeviceMessageHandler {


    @Override
    public void handle(DeviceMessage deviceMessage) {
        String deviceId = deviceMessage.getDeviceId();
        String scope = deviceMessage.getScope();
        String message = deviceMessage.getMessage();

        log.info("Handling DEVICE_DATA scope [{}] deviceId [{}] message [{}]", scope, deviceId, message);
    }
}
