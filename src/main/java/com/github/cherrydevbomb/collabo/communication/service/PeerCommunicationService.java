package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.subscriber.PeerInitStateTransferSubscriber;
import com.github.cherrydevbomb.collabo.communication.subscriber.RemoteDocumentChangeSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import com.github.cherrydevbomb.collabo.communication.util.UserIdGenerator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.util.concurrent.ExecutionException;

public class PeerCommunicationService extends CommunicationService {

    private static PeerCommunicationService peerCommunicationService;

    RedisPubSubListener<String, String> peerInitStateTransferSubscriber;
    RedisPubSubListener<String, String> remoteDocumentChangeSubscriber;

    public static PeerCommunicationService getInstance() {
        if (peerCommunicationService == null) {
            peerCommunicationService = new PeerCommunicationService();
        }
        return peerCommunicationService;
    }

    public boolean isActivePeerSession() {
        return currentSessionId != null;
    }

    public void joinSession(String sessionId, Project project) throws ExecutionException, InterruptedException {
        if (isActivePeerSession()) {
            return;
        }

        this.userId = UserIdGenerator.generate();
        this.currentSessionId = sessionId;
        String initStateRequestChannel = ChannelUtil.getChannel(sessionId, ChannelType.INIT_STATE_REQUEST_CHANNEL);
        String initStateTransferChannel = ChannelUtil.getChannel(sessionId, ChannelType.INIT_STATE_TRANSFER_CHANNEL);
        String deleteAckChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DELETE_ACK_CHANNEL);

        DeleteAckService.getInstance().setDeleteAckChannel(deleteAckChannel);

        peerInitStateTransferSubscriber = new PeerInitStateTransferSubscriber(this, project);
        redisSubConnection.addListener(peerInitStateTransferSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(initStateTransferChannel);

        RedisPubSubAsyncCommands<String, String> asyncPub = redisPubConnection.async();
        long subscriberCount = asyncPub.publish(initStateRequestChannel, "Peer requests to join session " + sessionId).get();
        if (subscriberCount == 0) {
            throw new RuntimeException("InvalidSessionId");
        }
    }

    public void subscribeToChanges(Editor editor) {
        // unsubscribe from initial state transfer
        String initStateTransferChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.INIT_STATE_TRANSFER_CHANNEL);
        unsubscribe(initStateTransferChannel, peerInitStateTransferSubscriber);
        peerInitStateTransferSubscriber = null;

        // subscribe to future document changes
        String documentChangeChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DOCUMENT_CHANGE_CHANNEL);
        remoteDocumentChangeSubscriber = new RemoteDocumentChangeSubscriber(editor);
        redisSubConnection.addListener(remoteDocumentChangeSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(documentChangeChannel);
    }

    public void subscribeToHeartbeat() {
        subscribeToHeartbeatChannel();
        startHeartbeat();
    }

    public void leaveSession() {
        String documentChangeChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DOCUMENT_CHANGE_CHANNEL);
        unsubscribe(documentChangeChannel, remoteDocumentChangeSubscriber);
        remoteDocumentChangeSubscriber = null;

        stopHeartbeat();
        unsubscribeHeartbeat();
        unsubscribeFromDeleteAckChannel();

        invalidateSession();
        // TODO maybe close editor with shared file?
    }

    private void unsubscribe(String channel, RedisPubSubListener<String, String> subscriber) {
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(channel);
        redisSubConnection.removeListener(subscriber);
    }
}
