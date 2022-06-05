package com.github.cherrydevbomb.collabo.communication.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cherrydevbomb.collabo.communication.model.DocumentChange;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import com.github.cherrydevbomb.collabo.editor.EditorUtil;
import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Computable;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class RemoteDocumentChangeSubscriber implements RedisPubSubListener<String, String> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Editor editor;
    private final DocumentManager documentManager;
    private final BlockingQueue<Element> pendingDeleteQueue;

    private ScheduledExecutorService queueProcessingExecutor;

    public RemoteDocumentChangeSubscriber(Editor editor) {
        this.editor = editor;
        this.documentManager = DocumentManager.getInstance();
        this.pendingDeleteQueue = new LinkedBlockingQueue<>();
        startDeleteQueueProcessing();
    }

    @Override
    synchronized
    public void message(String channel, String message) {
        if (!ChannelUtil.isOfType(channel, ChannelType.DOCUMENT_CHANGE_CHANNEL)) {
            return;
        }

        DocumentChange documentChange;
        try {
            documentChange = mapper.readValue(message, DocumentChange.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading InitialState object from JSON", e);
            return; // TODO throw exception
        }

        // only process if the change was broadcasted by another user
        if (documentManager.getCurrentUserId().equals(documentChange.getInitiator())) {
            return;
        }

        switch (documentChange.getChangeType()) {
            case INSERT:
                documentManager.insertElement(documentChange.getElement());
                //adjust caret position if it matches insert event's offset
                Computable<Integer> getCaretOffsetLambda = () -> editor.getCaretModel().getPrimaryCaret().getOffset();
                int caretOffset = ApplicationManager.getApplication().runReadAction(getCaretOffsetLambda);
                if (caretOffset == documentChange.getOriginalEventOffset()) {
                    Runnable runnable = () -> editor.getCaretModel().getPrimaryCaret().moveToOffset(caretOffset + 1);
                    WriteCommandAction.runWriteCommandAction(editor.getProject(), runnable);
                }
                EditorUtil.insertText(editor, documentManager, documentChange.getElement());
                break;
            case DELETE:
                Element existingElement = documentManager.findElementById(documentChange.getElement().getId());
                if (existingElement != null) {
                    handleDelete(existingElement);
                } else {
                    enqueueDeletedElement(documentChange.getElement());
                }
                break;
        }
    }

    private void handleDelete(Element element) {
        documentManager.markElementAsDeleted(element);
        EditorUtil.deleteText(editor, documentManager, element);
    }

    private void enqueueDeletedElement(Element element) {
        try {
            pendingDeleteQueue.put(element);
        } catch (InterruptedException e) {
            log.error("Error adding message to queue.");
        }
    }

    private void startDeleteQueueProcessing() {
        queueProcessingExecutor = Executors.newScheduledThreadPool(1);
        queueProcessingExecutor.scheduleWithFixedDelay(this::processElementFromQueue, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void stopQueueProcessing() {
        queueProcessingExecutor.shutdown();
    }

    private void processElementFromQueue() {
        try {
            Element deletedElement = pendingDeleteQueue.take();
            Element existingElement = documentManager.findElementById(deletedElement.getId());
            if (existingElement != null) {
                handleDelete(existingElement);
            } else {
                enqueueDeletedElement(deletedElement);
            }
        } catch (InterruptedException e) {
            log.error("Error processing message from the queue.");
        }
    }

    @Override
    public void message(String pattern, String channel, String message) {

    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }
}
