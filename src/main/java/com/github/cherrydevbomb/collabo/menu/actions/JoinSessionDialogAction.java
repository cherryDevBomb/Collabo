package com.github.cherrydevbomb.collabo.menu.actions;

import com.github.cherrydevbomb.collabo.communication.service.PeerCommunicationService;
import com.github.cherrydevbomb.collabo.menu.validator.SessionIdValidator;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class JoinSessionDialogAction extends AnAction {

    private final PeerCommunicationService peerCommunicationService;

    public JoinSessionDialogAction() {
        super();
        peerCommunicationService = PeerCommunicationService.getInstance();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // TODO disable when hosting a session
        event.getPresentation().setEnabled(!peerCommunicationService.isActiveSession());
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
