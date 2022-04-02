package com.github.cherrydevbomb.collabo.menu.actions;

import com.github.cherrydevbomb.collabo.communication.service.HostCommunicationService;
import com.github.cherrydevbomb.collabo.communication.service.PeerCommunicationService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class StopSessionDialogAction extends AnAction {

    private final HostCommunicationService hostCommunicationService;
    private final PeerCommunicationService peerCommunicationService;

    public StopSessionDialogAction() {
        super();
        this.hostCommunicationService = HostCommunicationService.getInstance();
        peerCommunicationService = PeerCommunicationService.getInstance();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // enable only if participating in a session
        boolean enableAction = hostCommunicationService.isHostingSession() || peerCommunicationService.isActivePeerSession();
        event.getPresentation().setEnabled(enableAction);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project currentProject = event.getProject();

        String description = hostCommunicationService.isHostingSession() ? "Are you sure you want to stop this session?" : "Are you sure you want to leave this session?";
        String title = event.getPresentation().getDescription();

        int selected = Messages.showOkCancelDialog(currentProject, description, title, "Yes", "No", Messages.getQuestionIcon());

        if (selected == Messages.OK) {
            try {
                hostCommunicationService.stopSession();
                peerCommunicationService.leaveSession();
            } catch (Exception e) {
                System.out.println("Error stopping session");
            }
        } else if (selected == Messages.CANCEL) {
            System.out.println("Stop session aborted");
        }
    }
}
