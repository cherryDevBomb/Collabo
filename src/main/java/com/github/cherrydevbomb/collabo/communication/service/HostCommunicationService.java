package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.subscriber.HostInitStateRequestSubscriber;
import com.github.cherrydevbomb.collabo.communication.subscriber.RemoteDocumentChangeSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import com.github.cherrydevbomb.collabo.communication.util.UserIdGenerator;
import com.github.cherrydevbomb.collabo.editor.LocalDocumentChangeListener;
import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class HostCommunicationService extends CommunicationService {

    private static HostCommunicationService hostCommunicationService;

    private RedisPubSubListener<String, String> hostInitStateRequestSubscriber;
    private RedisPubSubListener<String, String> remoteDocumentChangeSubscriber;

    private DocumentManager documentManager;

    public static HostCommunicationService getInstance() {
        if (hostCommunicationService == null) {
            hostCommunicationService = new HostCommunicationService();
        }
        return hostCommunicationService;
    }

    private HostCommunicationService() {
        documentManager = DocumentManager.getInstance();
    }

    public boolean isHostingSession() {
        return currentSessionId != null;
    }

    public void startSession(String sessionId, VirtualFile virtualFile, Project project) {
        if (isHostingSession()) {
            return; // TODO throw exception
        }

        userId = UserIdGenerator.generate();
        currentSessionId = sessionId;
        String initStateRequestChannel = ChannelUtil.getChannel(sessionId, ChannelType.INIT_STATE_REQUEST_CHANNEL);
        String initStateTransferChannel = ChannelUtil.getChannel(sessionId, ChannelType.INIT_STATE_TRANSFER_CHANNEL);
        String documentChangeChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DOCUMENT_CHANGE_CHANNEL);

        hostInitStateRequestSubscriber = new HostInitStateRequestSubscriber(initStateTransferChannel, virtualFile);
        redisSubConnection.addListener(hostInitStateRequestSubscriber);
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.subscribe(initStateRequestChannel);

        // init CRDT text array
        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(virtualFile));
        documentManager.setCurrentUserId(userId);
        documentManager.init(document.getText());

        // add listener for changes in the document
        Editor editor = ReadAction.compute(() -> FileEditorManager.getInstance(project).getSelectedTextEditor());
        document.addDocumentListener(new LocalDocumentChangeListener(editor, documentChangeChannel, userId, document.getText().length()));
        remoteDocumentChangeSubscriber = new RemoteDocumentChangeSubscriber(editor);
        redisSubConnection.addListener(remoteDocumentChangeSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(documentChangeChannel);

        // add listener for heartbeat and start broadcasting own heartbeat
        subscribeToHeartbeatChannel();
        startHeartbeat();
    }

    public void stopSession() {
        String initStateRequestChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.INIT_STATE_REQUEST_CHANNEL);
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(initStateRequestChannel);
        redisSubConnection.removeListener(hostInitStateRequestSubscriber);

        hostInitStateRequestSubscriber = null;

        stopHeartbeat();
        unsubscribeHeartbeat();

        invalidateSession();
        // TODO send message to peers to notify that session was stopped
    }
}
