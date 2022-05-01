package com.github.cherrydevbomb.collabo.communication.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cherrydevbomb.collabo.communication.model.Heartbeat;
import com.github.cherrydevbomb.collabo.communication.service.HeartbeatService;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartbeatSubscriber implements RedisPubSubListener<String, String> {

    private final ObjectMapper mapper;
    private final HeartbeatService heartbeatService;

    public HeartbeatSubscriber() {
        mapper = new ObjectMapper();
        heartbeatService = HeartbeatService.getInstance();
    }

    @Override
    public void message(String channel, String message) {
        if (!ChannelUtil.isOfType(channel, ChannelType.HEARTBEAT_CHANNEL)) {
            return;
        }

        Heartbeat heartbeat = null;
        try {
            heartbeat = mapper.readValue(message, Heartbeat.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading heartbeat object from JSON", e);
        }

        if (heartbeat != null) {
            heartbeatService.addHeartbeat(heartbeat);
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
