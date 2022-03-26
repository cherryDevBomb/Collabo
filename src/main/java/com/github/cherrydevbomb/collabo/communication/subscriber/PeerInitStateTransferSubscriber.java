package com.github.cherrydevbomb.collabo.communication.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cherrydevbomb.collabo.communication.model.InitialState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PeerInitStateTransferSubscriber implements RedisPubSubListener<String, String> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Project project;

    public PeerInitStateTransferSubscriber(Project project) {
        this.project = project;
    }

    @Override
    public void message(String channel, String message) {
        System.out.println("Peer received initial state: " + message);

        InitialState initialState;
        try {
            initialState = mapper.readValue(message, InitialState.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading InitialState object from JSON", e);
            return; // TODO throw exception
        }

        FileType fileType = ReadAction.compute(() -> FileTypeRegistry.getInstance().getFileTypeByFileName(initialState.getFileName()));
        AtomicReference<PsiFile> psiFile = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> psiFile.set(WriteAction.compute(() -> PsiFileFactory.getInstance(project).createFileFromText(initialState.getFileName(), fileType, initialState.getText()))));
        Document document = ReadAction.compute(() -> PsiDocumentManager.getInstance(project).getDocument(psiFile.get()));
        // TODO document is null
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.compute(() -> EditorFactory.getInstance().createEditor(document, project, fileType, false)));
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
