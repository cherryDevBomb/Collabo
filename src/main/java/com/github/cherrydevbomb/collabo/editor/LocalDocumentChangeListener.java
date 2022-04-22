package com.github.cherrydevbomb.collabo.editor;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.ChangeType;
import com.github.cherrydevbomb.collabo.communication.model.DocumentChange;
import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
import com.github.cherrydevbomb.collabo.editor.crdt.ID;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class LocalDocumentChangeListener implements DocumentListener {

    private final Editor editor;
    private final DocumentManager documentManager;
    private final StatefulRedisPubSubConnection<String, String> redisConnection;
    private final String documentChangeChannel;
    private final String userId;
    private final AtomicInteger operationCounter;

    public LocalDocumentChangeListener(Editor editor, String documentChangeChannel, String userId, int initialOpCounter) {
        this.editor = editor;
        this.documentManager = DocumentManager.getInstance();
        this.redisConnection = RedisConfig.getRedisPubConnection();
        this.documentChangeChannel = documentChangeChannel;
        this.userId = userId;
        this.operationCounter = new AtomicInteger(initialOpCounter);
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
        // if a change contains multiple characters, each character will be treated as a separate DocumentChange
        List<DocumentChange> documentChanges = new ArrayList<>();
        if (event.getOldLength() == 0) {
            documentChanges = handleInsertEvent(event);
        } else if (event.getNewLength() == 0) {
            documentChanges = handleDeleteEvent(event);
        }

        RedisPubSubAsyncCommands<String, String> async = redisConnection.async();
        try {
            if (CollectionUtils.isNotEmpty(documentChanges)) {
                for (DocumentChange documentChange : documentChanges) {
                    long subscriberCount = async.publish(documentChangeChannel, documentChange.serialize()).get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not publish to channel {}", documentChangeChannel);
        }
    }

    private List<DocumentChange> handleInsertEvent(DocumentEvent event) {
        int changeOffset = event.getOffset();
        String changeValue = event.getNewFragment().toString();

        List<Element> newElements = new ArrayList<>();
        for (char c : changeValue.toCharArray()) {
            int timestamp = operationCounter.getAndIncrement();
            ID operationId = new ID(userId, timestamp);
            Element element = documentManager.buildNewElementToInsert(changeOffset, String.valueOf(c), operationId);
            newElements.add(element);
        }

        // insert into local copy
        for (Element element : newElements) {
            documentManager.insertElement(element);
        }

        return newElements.stream()
                .map(element -> DocumentChange.builder()
                        .changeType(ChangeType.INSERT)
                        .element(element)
                        .build())
                .collect(Collectors.toList());
    }

    private List<DocumentChange> handleDeleteEvent(DocumentEvent event) {
        return new ArrayList<>();
    }
}
