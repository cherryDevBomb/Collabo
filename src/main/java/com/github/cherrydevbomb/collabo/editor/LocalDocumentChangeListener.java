package com.github.cherrydevbomb.collabo.editor;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.ChangeType;
import com.github.cherrydevbomb.collabo.communication.model.DocumentChange;
import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
import com.github.cherrydevbomb.collabo.editor.crdt.ID;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LocalDocumentChangeListener implements DocumentListener {

    private final Editor editor;
    private final DocumentManager documentManager;
    private final StatefulRedisPubSubConnection<String, String> redisConnection;
    private final String documentChangeChannel;
    private final String userId;
    private final AtomicInteger operationCounter;

    public LocalDocumentChangeListener(Editor editor, String documentChangeChannel, String userId) {
        this.editor = editor;
        this.documentManager = DocumentManager.getInstance();
        this.redisConnection = RedisConfig.getRedisPubConnection();
        this.documentChangeChannel = documentChangeChannel;
        this.userId = userId;
        this.operationCounter = new AtomicInteger(0);
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        DocumentListener.super.documentChanged(event);

        int caretOffset = editor.getCaretModel().getPrimaryCaret().getOffset();

        if (caretOffset != event.getOffset()) {
            // the change is a result of an edit done by another peer
            return;
        }

        // broadcast event if document change was done on the current peer
        int changeOffset = event.getOffset();
        String changeValue = event.getNewFragment().toString();
        int timestamp = operationCounter.getAndIncrement();
        ID operationId = new ID(userId, timestamp);

        // TODO check if insert event
        Element element = documentManager.buildNewElementToInsert(changeOffset, changeValue, operationId);
        // insert into local copy
        documentManager.insertElement(element);
        EditorUtil.insertText(editor, documentManager.getElementOffset(element), element.getValue());

        DocumentChange documentChange = DocumentChange.builder()
                .changeType(ChangeType.INSERT)
                .element(element)
                .build();

        // TODO handle delete

        RedisPubSubAsyncCommands<String, String> async = redisConnection.async();
        try {
            long subscriberCount = async.publish(documentChangeChannel, documentChange.serialize()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not publish to channel {}", documentChangeChannel);
        }
    }
}
