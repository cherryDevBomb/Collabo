package com.github.cherrydevbomb.collabo.menu.validator;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.NlsSafe;

public class SessionIdValidator implements InputValidator {

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
        return true;
    }
}
