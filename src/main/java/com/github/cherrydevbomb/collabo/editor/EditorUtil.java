package com.github.cherrydevbomb.collabo.editor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public class EditorUtil {

    public static void insertText(Editor editor, int offset, String text) {
        Document document = editor.getDocument();
        Project project = editor.getProject();
        Runnable runnable = () -> document.insertString(offset, text);
        WriteCommandAction.runWriteCommandAction(project, runnable);
    }
}
