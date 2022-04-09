package com.github.cherrydevbomb.collabo.editor.crdt;

import java.util.List;

public class DocumentManager {

    private static DocumentManager documentManager;

    private List<Element> textElements;

    public static DocumentManager getInstance() {
        if (documentManager == null) {
            documentManager = new DocumentManager();
        }
        return documentManager;
    }

    private DocumentManager() {
    }

//    public List<String> getText() {
//        return null;
//    }

    /**
     * Builds a new element to be inserted by calculating its left and right origins.
     *
     * @param offset      offset of insertion inside the editor
     * @param value       inserted value
     * @param operationId unique identifier
     * @return element built with the given data
     */
    public Element buildNewElementToInsert(int offset, String value, ID operationId) {
        int positionIndex = findElementIndexByOffset(offset);
        ID left = getElementAt(positionIndex - 1).getId();
        ID right = getElementAt(positionIndex).getId();

        return Element.builder()
                .id(operationId)
                .originLeft(left)
                .originRight(right)
                .value(value)
                .build();
    }

    /**
     * Inserts a given element into the list based on its left and right origins.
     *
     * @param toInsert elsement to insert
     */
    public void insertElement(Element toInsert) {
        //assuming S = originLeft, c1, c2, ..., cn, originRight, then we say that element conflicts with c1, ..., cn
        int originLeft = textElements.indexOf(findElementById(toInsert.getOriginLeft()));
        int originRight = textElements.indexOf(findElementById(toInsert.getOriginRight()));
        List<Element> conflictingOperations = textElements.subList(originLeft + 1, originRight);

        int insertPosition = originLeft + 1;
        // Rule 2 - transitivity
        // advance position as long as conflict < element
        for (Element conflict : conflictingOperations) {
            int indexConflict = getElementIndex(conflict.getId());
            int originConflict = getElementIndex(conflict.getOriginLeft());
            int originToInsert = getElementIndex(toInsert.getOriginLeft());

            // Rule 1: o1 < o2 <=> o1 < origin2 OR origin2 <= origin1
            boolean rule1 = indexConflict < originToInsert || originToInsert <= originConflict;

            // Rule 3: o1 < o2 <=> origin1 == origin2 OR creator1 < creator2
            boolean rule3 = originConflict != originToInsert || conflict.getId().isPredecessorOf(toInsert.getId());

            if (rule1 && rule3) {
                // toInsert is a successor of conflict
                insertPosition++;
            } else if (originToInsert > originConflict) {
                // rule1 no longer satisfied -> origins would cross
                break;
            }
        }
        textElements.add(insertPosition, toInsert);
    }

    /**
     * Returns the element positioned at the given index, checking if the index is within list bounds.
     *
     * @param index integer value
     * @return element at the given index, or null if out of bounds
     */
    public Element getElementAt(int index) {
        if (index < 0 || index >= textElements.size()) {
            return null;
        }
        return textElements.get(index);
    }

    /**
     * Gets the position of the element with the given id in the textElements list
     *
     * @param id of the element to search for
     * @return position in the textElements list
     */
    public int getElementIndex(ID id) {
        Element element = findElementById(id);
        return textElements.indexOf(element);
    }

    /**
     * Maps the index of a character inside the editor into an actual element index, skipping over deleted elements.
     *
     * @param offset offset of a character inside the editor
     * @return index of the corresponding element
     */
    public int findElementIndexByOffset(int offset) {
        int notDeleted = 0;
        int i = 0;
        while (notDeleted < offset) {
            if (!textElements.get(i).isDeleted()) {
                notDeleted++;
            }
            i++;
        }
        return i;
    }

    public int getElementOffset(Element element) {
        int elementIndex = getElementIndex(element.getId());
        int offset = 0;
        int i = 0;
        while (i < elementIndex) {
            if (!textElements.get(i).isDeleted()) {
                offset++;
            }
            i++;
        }
        return offset;
    }

    private Element findElementById(ID id) {
        return textElements.stream()
                .filter(elem -> elem.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
