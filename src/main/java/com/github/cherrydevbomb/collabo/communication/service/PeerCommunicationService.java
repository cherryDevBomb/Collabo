package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.subscriber.PeerInitStateTransferSubscriber;
import com.github.cherrydevbomb.collabo.communication.subscriber.RemoteDocumentChangeSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import com.intellij.openapi.editor.Editor;
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
    RedisPubSubListener<String, String> remoteDocumentChangeSubscriber;

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

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public boolean isActivePeerSession() {
        return currentSessionId != null;
    }

    public void joinSession(String sessionId, Project project) {
        if (isActivePeerSession()) {
            return;
        }

        this.currentSessionId = sessionId;
        String initStateRequestChannel = ChannelUtil.getInitStateRequestChannel(sessionId);
        String initStateTransferChannel = ChannelUtil.getInitStateTransferChannel(sessionId);

        peerInitStateTransferSubscriber = new PeerInitStateTransferSubscriber(this, project);
        redisSubConnection.addListener(peerInitStateTransferSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(initStateTransferChannel);

        // TODO form session request message
        RedisPubSubAsyncCommands<String, String> asyncPub = redisPubConnection.async();
        asyncPub.publish(initStateRequestChannel, "Peer requests to join session " + sessionId);
    }

    public void subscribeToChanges(Editor editor) {
        // unsubscribe from initial state transfer
        String initStateTransferChannel = ChannelUtil.getInitStateTransferChannel(currentSessionId);
        unsubscribe(initStateTransferChannel, peerInitStateTransferSubscriber);
        peerInitStateTransferSubscriber = null;

        // subscribe to future document changes
        String documentChangeChannel = ChannelUtil.getDocumentChangeChannel(currentSessionId);
        remoteDocumentChangeSubscriber = new RemoteDocumentChangeSubscriber(editor);
        redisSubConnection.addListener(remoteDocumentChangeSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(documentChangeChannel);
    }

    public void leaveSession() {
        String documentChangeChannel = ChannelUtil.getDocumentChangeChannel(currentSessionId);
        unsubscribe(documentChangeChannel, remoteDocumentChangeSubscriber);
        remoteDocumentChangeSubscriber = null;

        currentSessionId = null;

        // TODO maybe close editor with shared file?
    }

    private void unsubscribe(String channel, RedisPubSubListener<String, String> subscriber) {
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(channel);
        redisSubConnection.removeListener(subscriber);
    }
}
