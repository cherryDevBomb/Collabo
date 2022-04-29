package com.github.cherrydevbomb.collabo.editor;

import com.github.cherrydevbomb.collabo.editor.crdt.DocumentManager;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;

public class EditorUtil {

    public static void insertText(Editor editor, DocumentManager documentManager, Element element) {
        Document document = editor.getDocument();
        Project project = editor.getProject();
        WriteCommandAction.runWriteCommandAction(project, () -> {
                int offset = documentManager.getElementOffset(element);
                document.insertString(offset, element.getValue());
        });
    }

    public static void deleteText(Editor editor, DocumentManager documentManager, Element element) {
        Document document = editor.getDocument();
        Project project = editor.getProject();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            int offset = documentManager.getElementOffset(element);
            // double check that the value inside the editor at delete offset corresponds to the value that needs to be deleted
            String actualValue = document.getText(TextRange.from(offset, element.getValue().length()));
            if (element.getValue().equals(actualValue)) {
                document.deleteString(offset, offset + element.getValue().length());
            }
        });
    }
}
