package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.subscriber.HostInitStateRequestSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class HostCommunicationService {

    private static HostCommunicationService hostCommunicationService;

    private String currentSessionId;

    private StatefulRedisPubSubConnection<String, String> redisPubConnection;
    private StatefulRedisPubSubConnection<String, String> redisSubConnection;

    RedisPubSubListener<String, String> hostInitStateRequestSubscriber;

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

    public void startSession(String sessionId, VirtualFile virtualFile) {
        if (isHostingSession()) {
            return; // TODO throw exception
        }

        currentSessionId = sessionId;
        String initStateRequestChannel = ChannelUtil.getInitStateRequestChannel(sessionId);
        String initStateTransferChannel = ChannelUtil.getInitStateTransferChannel(sessionId);

        hostInitStateRequestSubscriber = new HostInitStateRequestSubscriber(initStateTransferChannel, virtualFile);
        redisSubConnection.addListener(hostInitStateRequestSubscriber);
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.subscribe(initStateRequestChannel);
    }

    public void stopSession() {
        String initStateRequestChannel = ChannelUtil.getInitStateRequestChannel(currentSessionId);
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(initStateRequestChannel);
        redisSubConnection.removeListener(hostInitStateRequestSubscriber);

        hostInitStateRequestSubscriber = null;
        currentSessionId = null;

        // TODO send message to peers to notify that session was stopped
    }
}
