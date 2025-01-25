package com.mawippel.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderCommandResponse {

    private String command;
    private String response;
    private String command_id;
    private JsonNode payload;
    private String deviceId;

}
