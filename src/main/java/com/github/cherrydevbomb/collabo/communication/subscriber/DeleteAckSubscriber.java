package com.github.cherrydevbomb.collabo.communication.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cherrydevbomb.collabo.communication.model.DeleteAck;
import com.github.cherrydevbomb.collabo.communication.service.DeleteAckService;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.github.cherrydevbomb.collabo.editor.crdt.ID;
import com.github.cherrydevbomb.collabo.persistence.DBLogger;
import com.github.cherrydevbomb.collabo.persistence.Table;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteAckSubscriber implements RedisPubSubListener<String, String> {

    private final ObjectMapper mapper;
    private final DeleteAckService deleteAckService;
    private final DocumentManager documentManager;
    private final DBLogger dbLogger;

    public DeleteAckSubscriber() {
        this.mapper = new ObjectMapper();
        this.deleteAckService = DeleteAckService.getInstance();
        this.documentManager = DocumentManager.getInstance();
        this.dbLogger = DBLogger.getInstance();
    }

    @Override
    public void message(String channel, String message) {
        if (!ChannelUtil.isOfType(channel, ChannelType.DELETE_ACK_CHANNEL)) {
            return;
        }

        DeleteAck deleteAck = null;
        try {
            deleteAck = mapper.readValue(message, DeleteAck.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading deleteAck object from JSON", e);
        }

        if (deleteAck == null) {
            return;
        }

        deleteAckService.addDeleteAck(deleteAck);
        ID elementId = deleteAck.getElementId();
        if (deleteAckService.allAcksReceived(elementId)) {
            documentManager.garbageCollectElement(elementId);
            deleteAckService.removeElementFromAckMap(elementId);
            dbLogger.log(Table.GARBAGE_COLLECT, elementId.toString(), documentManager.getCurrentUserId(), System.currentTimeMillis());
        }
    }

    @Override
    public void message(String pattern, String channel, String message) {

    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }
}
