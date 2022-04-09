package com.github.cherrydevbomb.collabo.communication.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
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
public class DocumentChange {

    private Element element;
    private ChangeType changeType;

    public String serialize() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Error converting DocumentChange object to JSON", e);
        }
        return "";
    }
}
