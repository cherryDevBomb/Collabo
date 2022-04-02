package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.subscriber.PeerInitStateTransferSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelNameBuilder;
import com.intellij.openapi.project.Project;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class PeerCommunicationService {

    private static PeerCommunicationService peerCommunicationService;

    private String currentSessionId;

    private final StatefulRedisPubSubConnection<String, String> redisPubConnection;
    private final StatefulRedisPubSubConnection<String, String> redisSubConnection;

    RedisPubSubListener<String, String> peerInitStateTransferSubscriber;

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

    public boolean isActivePeerSession() {
        return currentSessionId != null;
    }

    public void joinSession(String sessionId, Project project) {
        if (isActivePeerSession()) {
            return;
        }

        this.currentSessionId = sessionId;
        String initStateRequestChannel = ChannelNameBuilder.getInitStateRequestChannel(sessionId);
        String initStateTransferChannel = ChannelNameBuilder.getInitStateTransferChannel(sessionId);

        peerInitStateTransferSubscriber = new PeerInitStateTransferSubscriber(project);
        redisSubConnection.addListener(peerInitStateTransferSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(initStateTransferChannel);

        // TODO form session request message
        RedisPubSubAsyncCommands<String, String> asyncPub = redisPubConnection.async();
        asyncPub.publish(initStateRequestChannel, "Peer requests to join session " + sessionId);
    }

    public void leaveSession() {
        String initStateTransferChannel = ChannelNameBuilder.getInitStateTransferChannel(currentSessionId);
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(initStateTransferChannel);
        redisSubConnection.removeListener(peerInitStateTransferSubscriber);

        peerInitStateTransferSubscriber = null;
        currentSessionId = null;

        // TODO maybe close editor with shared file?
    }
}
