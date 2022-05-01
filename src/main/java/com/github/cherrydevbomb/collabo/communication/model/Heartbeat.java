package com.github.cherrydevbomb.collabo.communication.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class Heartbeat {

    private String userId;
    private long timestamp;

    public String serialize() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Error converting Heartbeat object to JSON", e);
        }
        return "";
    }
}
