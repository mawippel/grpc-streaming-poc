package com.mawippel.services.handlers;

import com.mawippel.domain.DeviceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Class responsible for handling RFID Readers' messages.
 * The behavior of this class was copied from ReaderCommandRespService.java (inside outbound-multiplex-service)
 */
@Service
@Slf4j
public class DeviceActionResponseHandler implements DeviceMessageHandler {

    @Override
    public void handle(DeviceMessage deviceMessage) {
        var scope = deviceMessage.getScope();
        var deviceId = deviceMessage.getDeviceId();
        var message = deviceMessage.getMessage();
        log.info("Handling DEVICE_ACTION_RESPONSE scope [{}] deviceId [{}]", scope, deviceId);
        log.info("Message [{}]", message);
    }

}
