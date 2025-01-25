package com.mawippel.services.handlers;

import com.mawippel.domain.DeviceMessage;

public interface DeviceMessageHandler {

    void handle(DeviceMessage deviceMessage);
}
