package com.github.cherrydevbomb.collabo.editor;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.ChangeType;
import com.github.cherrydevbomb.collabo.communication.model.DeleteAck;
import com.github.cherrydevbomb.collabo.communication.model.DocumentChange;
import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
import com.github.cherrydevbomb.collabo.editor.crdt.ID;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Computable;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
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
    synchronized
    public void documentChanged(@NotNull DocumentEvent event) {
        Computable<Integer> getCaretOffsetLambda = () -> editor.getCaretModel().getPrimaryCaret().getOffset();
        int caretOffset = ApplicationManager.getApplication().runReadAction(getCaretOffsetLambda);

        String changeValue = event.getOldLength() == 0 ? event.getNewFragment().toString() : event.getOldFragment().toString();
        if (changeValue.length() > 1) {
            // only local changes can have a length of more than 1 character
        } else {
            if (caretOffset != event.getOffset()) {
                // the change is a result of an edit done by another peer
                return;
            }
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

        if (CollectionUtils.isNotEmpty(documentChanges)) {
            for (DocumentChange documentChange : documentChanges) {
                try {
                    long subscriberCount = async.publish(documentChangeChannel, documentChange.serialize()).get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Could not publish to channel {}", documentChangeChannel);
                }
            }
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

        return buildDocumentChangeEventList(newElements, ChangeType.INSERT, event.getOffset());
    }

    private List<DocumentChange> handleDeleteEvent(DocumentEvent event) {
        int changeOffset = event.getOffset();
        String changeValue = event.getOldFragment().toString();
        char[] changeValueCharArray = changeValue.toCharArray();

        List<Element> deletedElements = new ArrayList<>();

        int deletedCharIndex = documentManager.findElementIndexByOffset(changeOffset);
        if (deletedCharIndex == -1) {
            return Collections.emptyList();
        }

        Element deletedElement = documentManager.markElementAsDeleted(deletedCharIndex, String.valueOf(changeValueCharArray[0]));
        if (deletedElement.isDeleted()) {
            deletedElements.add(deletedElement);
        }
        for (int i = 1; i < changeValueCharArray.length; i++) {
            deletedCharIndex = documentManager.findIndexOfNextNotDeletedElement(deletedCharIndex);
            Element nextDeletedElement = documentManager.markElementAsDeleted(deletedCharIndex, String.valueOf(changeValueCharArray[i]));
            if (nextDeletedElement != null && nextDeletedElement.isDeleted()) {
                deletedElements.add(nextDeletedElement);
            }
        }

        return buildDocumentChangeEventList(deletedElements, ChangeType.DELETE, event.getOffset());
    }

    private List<DocumentChange> buildDocumentChangeEventList(List<Element> elements, ChangeType changeType, int eventOffset) {
        return elements.stream()
                .map(element -> DocumentChange.builder()
                        .changeType(changeType)
                        .element(element)
                        .initiator(userId)
                        .originalEventOffset(eventOffset)
                        .build())
                .collect(Collectors.toList());
    }
}
