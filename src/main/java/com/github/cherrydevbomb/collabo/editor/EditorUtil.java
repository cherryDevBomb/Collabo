package com.github.cherrydevbomb.collabo.editor;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;

public class EditorUtil {

    public static void insertText(Editor editor, int offset, String text) {
        Document document = editor.getDocument();
        Project project = editor.getProject();
        Runnable runnable = () -> document.insertString(offset, text);
        WriteCommandAction.runWriteCommandAction(project, runnable);
    }

    public static void deleteText(Editor editor, int offset, String value) {
        Document document = editor.getDocument();
        Project project = editor.getProject();
        Runnable runnable = () -> document.deleteString(offset, offset + value.length());

        // double check that the value inside the editor at delete offset corresponds to the value that needs to be deleted
        String actualValue = document.getText(TextRange.from(offset, value.length()));
        if (value.equals(actualValue)) {
            WriteCommandAction.runWriteCommandAction(project, runnable);
        }
    }
}
