package com.github.cherrydevbomb.collabo.menu.validator;

import com.github.cherrydevbomb.collabo.communication.service.PeerCommunicationService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.NlsSafe;

public class SessionIdValidator implements InputValidator {

    private PeerCommunicationService peerCommunicationService;
    private Project project;

    public SessionIdValidator(Project project) {
        super();
        this.peerCommunicationService = PeerCommunicationService.getInstance();
        this.project = project;
    }

    @Override
    public boolean checkInput(@NlsSafe String inputString) {
        // TODO implement validation of sessionId
        System.out.println("Input checked");
        return true;
    }

    @Override
    public boolean canClose(@NlsSafe String inputString) {
        // TODO trigger connecting to session
        System.out.println(inputString);

        peerCommunicationService.joinSession(inputString, project);

        return true;
    }
}
