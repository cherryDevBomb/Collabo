package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.subscriber.PeerInitStateTransferSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelNameBuilder;
import com.intellij.openapi.project.Project;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class PeerCommunicationService {

    private static PeerCommunicationService peerCommunicationService;

    private String currentSessionId;

    private StatefulRedisPubSubConnection<String, String> redisPubConnection;
    private StatefulRedisPubSubConnection<String, String> redisSubConnection;

    public static PeerCommunicationService getInstance() {
        if (peerCommunicationService == null) {
            peerCommunicationService = new PeerCommunicationService();
        }
        return peerCommunicationService;
    }

    private PeerCommunicationService() {
        redisPubConnection = RedisConfig.getRedisPubConnection();
        redisSubConnection = RedisConfig.getRedisSubConnection();
    }

    public boolean isActiveSession() {
        return currentSessionId != null;
    }

    public void joinSession(String sessionId, Project project) {
        if (isActiveSession()) {
            return;
        }

        this.currentSessionId = sessionId;
        String initStateRequestChannel = ChannelNameBuilder.getInitStateRequestChannel(sessionId);
        String initStateTransferChannel = ChannelNameBuilder.getInitStateTransferChannel(sessionId);

        redisSubConnection.addListener(new PeerInitStateTransferSubscriber(project));
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(initStateTransferChannel);

        // TODO form session request message
        RedisPubSubAsyncCommands<String, String> asyncPub = redisPubConnection.async();
        asyncPub.publish(initStateRequestChannel, "Peer requests to join session " + sessionId);
    }

    //TODO implement leave session
    public void leaveSession() {

    }
}
