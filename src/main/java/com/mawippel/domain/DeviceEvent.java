package com.mawippel.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceEvent {

    private String eventDataString;
    private String eventType;

}
