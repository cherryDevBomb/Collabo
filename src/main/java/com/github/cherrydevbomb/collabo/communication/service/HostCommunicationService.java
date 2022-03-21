package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.subscriber.HostInitStateRequestSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelNameBuilder;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class HostCommunicationService {

    private static HostCommunicationService hostCommunicationService;

    private String currentSessionId;

    private StatefulRedisPubSubConnection<String, String> redisPubConnection;
    private StatefulRedisPubSubConnection<String, String> redisSubConnection;

    public static HostCommunicationService getInstance() {
        if (hostCommunicationService == null) {
            hostCommunicationService = new HostCommunicationService();
        }
        return hostCommunicationService;
    }

    private HostCommunicationService() {
        redisPubConnection = RedisConfig.getRedisPubConnection();
        redisSubConnection = RedisConfig.getRedisSubConnection();
    }

    public boolean isHostingSession() {
        return currentSessionId != null;
    }

    public void startSession(String sessionId) {
        if (isHostingSession()) {
            return; // TODO throw exception
        }

        this.currentSessionId = sessionId;
        String initStateRequestChannel = ChannelNameBuilder.getInitStateRequestChannel(sessionId);
        String initStateTransferChannel = ChannelNameBuilder.getInitStateTransferChannel(sessionId);

        redisSubConnection.addListener(new HostInitStateRequestSubscriber(initStateTransferChannel));
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.subscribe(initStateRequestChannel);
    }

    //TODO implement destroy session
    public void destroySession() {

    }
}
