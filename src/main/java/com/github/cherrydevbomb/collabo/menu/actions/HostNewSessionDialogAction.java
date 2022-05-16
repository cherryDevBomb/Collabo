package com.github.cherrydevbomb.collabo.menu.actions;

import com.github.cherrydevbomb.collabo.communication.service.HostCommunicationService;
import com.github.cherrydevbomb.collabo.communication.service.PeerCommunicationService;
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
    private final PeerCommunicationService peerCommunicationService;

    public HostNewSessionDialogAction() {
        super();
        this.hostCommunicationService = HostCommunicationService.getInstance();
        this.peerCommunicationService = PeerCommunicationService.getInstance();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // enable only if a project is open
        Project currentProject = event.getProject();
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        event.getPresentation().setEnabled(currentProject != null && virtualFile != null && !hostCommunicationService.isHostingSession() && !peerCommunicationService.isActivePeerSession());
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
                VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
                hostCommunicationService.startSession(sessionId, virtualFile, currentProject);
            } catch (Exception e) {
                System.out.println("Error starting new session");
                return;
            }
            System.out.println("Session started");
        } else if (selected == Messages.CANCEL) {
            System.out.println("Cancel clicked");
        }
    }
}
