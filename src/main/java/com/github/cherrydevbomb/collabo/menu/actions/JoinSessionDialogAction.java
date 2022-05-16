package com.github.cherrydevbomb.collabo.menu.actions;

import com.github.cherrydevbomb.collabo.communication.service.HostCommunicationService;
import com.github.cherrydevbomb.collabo.communication.service.PeerCommunicationService;
import com.github.cherrydevbomb.collabo.menu.validator.SessionIdValidator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class JoinSessionDialogAction extends AnAction {

    private final PeerCommunicationService peerCommunicationService;
    private final HostCommunicationService hostCommunicationService;

    public JoinSessionDialogAction() {
        super();
        peerCommunicationService = PeerCommunicationService.getInstance();
        hostCommunicationService = HostCommunicationService.getInstance();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(!peerCommunicationService.isActivePeerSession() && !hostCommunicationService.isHostingSession());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // Create and show a dialog
        Project currentProject = event.getProject();
        String message = "Enter the ID you received to join a Collabo session";
        String title = event.getPresentation().getDescription();
        Messages.showInputDialog(currentProject, message, title, Messages.getInformationIcon(), "", new SessionIdValidator(currentProject));
    }
}
