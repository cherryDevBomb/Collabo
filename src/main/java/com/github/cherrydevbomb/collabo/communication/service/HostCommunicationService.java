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
        String deleteAckChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DELETE_ACK_CHANNEL);

        DeleteAckService.getInstance().setDeleteAckChannel(deleteAckChannel);

        // subscribe to INIT_STATE_REQUEST_CHANNEL
        hostInitStateRequestSubscriber = new HostInitStateRequestSubscriber(initStateTransferChannel, virtualFile);
        subscribe(initStateRequestChannel, hostInitStateRequestSubscriber);

        // init CRDT text array
        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(virtualFile));
        documentManager.setCurrentUserId(userId);
        documentManager.init(document.getText());

        // add listener for changes in the document
        Editor editor = ReadAction.compute(() -> FileEditorManager.getInstance(project).getSelectedTextEditor());
        document.addDocumentListener(new LocalDocumentChangeListener(editor, documentChangeChannel, userId, document.getText().length()));
        remoteDocumentChangeSubscriber = new RemoteDocumentChangeSubscriber(editor);
        subscribe(documentChangeChannel, remoteDocumentChangeSubscriber);

        // add listener for heartbeat and start broadcasting own heartbeat
        subscribeToHeartbeatChannel();
        startHeartbeat();

        // add listener for delete acknowledgements
        subscribeToDeleteAckChannel();
    }

    public void stopSession() {
        String initStateRequestChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.INIT_STATE_REQUEST_CHANNEL);
        unsubscribe(initStateRequestChannel, hostInitStateRequestSubscriber);
        unsubscribeAll();
        invalidateSession();
        // TODO send message to peers to notify that session was stopped
    }
}
