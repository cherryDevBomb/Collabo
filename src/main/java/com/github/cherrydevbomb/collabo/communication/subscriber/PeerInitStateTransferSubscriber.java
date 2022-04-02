package com.github.cherrydevbomb.collabo.communication.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cherrydevbomb.collabo.communication.model.InitialState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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

        ApplicationManager.getApplication().invokeLater(() -> {
            PsiDirectory projectRoot = ReadAction.compute(() -> PsiDirectoryFactory.getInstance(project).createDirectory(project.getProjectFile().getParent()));
            PsiDirectory existingCollaboDirectory = WriteAction.compute(() -> projectRoot.findSubdirectory("collabo"));
            PsiDirectory collaboDirectory = (existingCollaboDirectory != null) ? existingCollaboDirectory : WriteAction.compute(() -> projectRoot.createSubdirectory("collabo"));

            // delete existing files left over from previous sessions
            WriteAction.compute(() -> {
                Stream.of(collaboDirectory.getFiles()).forEach(file -> {
                    try {
                        file.getVirtualFile().delete(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                return null;
            });

            // create and show the transferred file in the editor
            WriteAction.compute(() -> collaboDirectory.add(psiFile.get()));
            VirtualFile virtualFile = ReadAction.compute(() -> collaboDirectory.findFile(initialState.getFileName()).getVirtualFile());
            ReadAction.compute(() -> FileEditorManager.getInstance(project).openFile(virtualFile, true));
            FileDocumentManager.getInstance().saveAllDocuments();
        });
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
