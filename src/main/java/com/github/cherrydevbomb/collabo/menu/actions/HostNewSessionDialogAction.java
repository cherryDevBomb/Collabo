package com.github.cherrydevbomb.collabo.menu.actions;

import com.github.cherrydevbomb.collabo.communication.service.HostCommunicationService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.UUID;

public class HostNewSessionDialogAction extends AnAction {

    private final HostCommunicationService hostCommunicationService;

    public HostNewSessionDialogAction() {
        super();
        this.hostCommunicationService = HostCommunicationService.getInstance();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // enable only if a project is open
        // TODO disable when editing in another session
        Project currentProject = event.getProject();
        event.getPresentation().setEnabled(currentProject != null && !hostCommunicationService.isHostingSession());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // Create and show a dialog
        Project currentProject = event.getProject();
        StringBuilder message = new StringBuilder("Created session for editing ");

        // If an element is selected in the editor, add info about it.
        Navigatable selectedElement = event.getData(CommonDataKeys.NAVIGATABLE);
        if (selectedElement != null) {
            message.append(selectedElement);
        }

        // Create sessionId
        String sessionId = UUID.randomUUID().toString();

        sessionId = "X"; //TODO remove after debug

        // Create dialog text
        message.append("\n\n")
                .append(sessionId)
                .append("\n\n")
                .append("Press Start to copy the session ID and start.");

        String title = event.getPresentation().getDescription();
        int selected = Messages.showOkCancelDialog(currentProject, message.toString(), title, "Start", "Cancel", Messages.getInformationIcon());

        if (selected == Messages.OK) {
            // copy sessionId to clipboard
            System.out.println("Pressed copy");
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(sessionId), null);

            try {
//                Document document = event.getData(CommonDataKeys.EDITOR).getDocument(); //TODO add null check or enable "start session" only if a document is open
                VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
                hostCommunicationService.startSession(sessionId, virtualFile);
            } catch (Exception e) {
                System.out.println("Error starting new session");
            }
            System.out.println("Session started");
        } else if (selected == Messages.CANCEL) {
            System.out.println("Cancel clicked");
        }
    }
}
