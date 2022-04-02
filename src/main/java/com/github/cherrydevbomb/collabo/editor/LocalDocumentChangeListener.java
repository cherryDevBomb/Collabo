package com.github.cherrydevbomb.collabo.editor;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.DocumentChange;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

@Slf4j
public class LocalDocumentChangeListener implements DocumentListener {

    private final Editor editor;
    private final StatefulRedisPubSubConnection<String, String> redisConnection;
    private final String documentChangeChannel;

    public LocalDocumentChangeListener(Editor editor, String documentChangeChannel) {
        this.editor = editor;
        redisConnection = RedisConfig.getRedisPubConnection();
        this.documentChangeChannel = documentChangeChannel;
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
        // TODO modify message to contain relevant fields
        DocumentChange documentChange = DocumentChange.builder()
                .offset(event.getOffset())
                .text(event.getNewFragment().toString())
                .build();

        RedisPubSubAsyncCommands<String, String> async = redisConnection.async();
        try {
            long subscriberCount = async.publish(documentChangeChannel, documentChange.serialize()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not publish to channel {}", documentChangeChannel);
        }
    }
}
