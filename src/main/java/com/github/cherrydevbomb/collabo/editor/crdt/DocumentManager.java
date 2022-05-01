package com.github.cherrydevbomb.collabo.editor.crdt;

import com.github.cherrydevbomb.collabo.communication.service.DeleteAckService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DocumentManager {

    private static DocumentManager documentManager;

    @Getter
    private List<Element> textElements;

    @Getter
    @Setter
    private String currentUserId;

    private final DeleteAckService deleteAckService;

    public static DocumentManager getInstance() {
        if (documentManager == null) {
            documentManager = new DocumentManager();
        }
        return documentManager;
    }

    private DocumentManager() {
        deleteAckService = DeleteAckService.getInstance();
    }

    /**
     * Transforms a string text to a list of elements. Used for creating initial state from a file content received by the host.
     *
     * @param text
     */
    public void init(String text) {
        textElements = Collections.synchronizedList(new ArrayList<>());
        int i = 0;
        for (char c : text.toCharArray()) {
            Element elem = Element.builder()
                    .id(new ID(currentUserId, i))
                    .originLeft((i > 0) ? new ID(currentUserId, i - 1) : null)
                    .originRight((i < text.length() - 1) ? new ID(currentUserId, i + 1) : null)
                    .value(String.valueOf(c))
                    .build();
            textElements.add(elem);
            i++;
        }
    }

    public void init(List<Element> elements) {
        this.textElements = elements;
    }

    public String getContentAsText() {
        return textElements.stream()
                .filter(elem -> !elem.isDeleted())
                .map(Element::getValue)
                .collect(Collectors.joining());
    }

    /**
     * Builds a new element to be inserted by calculating its left and right origins.
     *
     * @param offset      offset of insertion inside the editor
     * @param value       inserted value
     * @param operationId unique identifier
     * @return element built with the given data
     */
    public Element buildNewElementToInsert(int offset, String value, ID operationId) {
        Element leftNeighbour = findElementByOffset(offset - 1);
        Element rightNeighbour = findElementByOffset(offset); // not +1 because the new element is not inserted yet
        ID left = leftNeighbour != null ? leftNeighbour.getId() : null;
        ID right = rightNeighbour != null ? rightNeighbour.getId() : null;

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
     * @param toInsert element to insert
     */
    public void insertElement(Element toInsert) {
        //assuming S = originLeft, c1, c2, ..., cn, originRight, then we say that element conflicts with c1, ..., cn
        Element originLeftElement = findElementById(toInsert.getOriginLeft());
        int originLeft = originLeftElement != null ? textElements.indexOf(originLeftElement) : 0;
        Element originRightElement = findElementById(toInsert.getOriginRight());
        int originRight = originRightElement != null ? textElements.indexOf(originRightElement) : textElements.size();
        List<Element> conflictingOperations = textElements.subList(originLeft + 1, originRight); // TODO filter deleted?

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

    public Element markElementAsDeleted(int index, String value) {
        log.info("markElementAsDeleted: index=" + index + "; value=" + value.replace("\n", "\\n").replace(" ", "\\s"));

        Element element = getElementAt(index);
        if (element != null && element.getValue().equals(value)) {
            element.setDeleted(true);
            textElements.set(index, element);
            deleteAckService.sendDeleteAck(element, currentUserId);
        }
        log.info("elementFound: " + (element != null ? element.toString() : "null"));
        return element;
    }

    public void markElementAsDeleted(Element element) {
        Element existingElement = findElementById(element.getId());
        existingElement.setDeleted(true);
        deleteAckService.sendDeleteAck(existingElement, currentUserId);
    }

    public void garbageCollectElement(ID elementId) {
        Element element = findElementById(elementId);
        int elementIndex = textElements.indexOf(element);

        // reassign origins
        Element leftNeighbour = getElementAt(findIndexOfPreviousNotDeletedElement(elementIndex));
        Element rightNeighbour = getElementAt(findIndexOfNextNotDeletedElement(elementIndex));
        for (Element e : textElements) {
            if (e.getOriginLeft() != null && e.getOriginLeft().equals(elementId)) {
                e.setOriginLeft(leftNeighbour != null ? leftNeighbour.getId() : null);
            }
            if (e.getOriginRight() != null && e.getOriginRight().equals(elementId)) {
                e.setOriginRight(rightNeighbour != null ? rightNeighbour.getId() : null);
            }
        }

        textElements.remove(element);
    }

    /**
     * Given an index, find the index of next not deleted successor
     *
     * @param referenceIndex
     * @return index of next not deleted successor
     */
    public int findIndexOfNextNotDeletedElement(int referenceIndex) {
        int index = referenceIndex + 1;
        Element currentElement = getElementAt(index);
        while (currentElement != null && currentElement.isDeleted()) {
            index++;
            currentElement = getElementAt(index);
        }
        return index;
    }

    /**
     * Given an index, find the index of previous not deleted predecessor
     *
     * @param referenceIndex
     * @return index of previous not deleted predecessor
     */
    public int findIndexOfPreviousNotDeletedElement(int referenceIndex) {
        int index = referenceIndex - 1;
        Element currentElement = getElementAt(index);
        while (currentElement != null && currentElement.isDeleted()) {
            index--;
            currentElement = getElementAt(index);
        }
        return index;
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
        while (notDeleted < offset || textElements.get(i).isDeleted()) {
            if (!textElements.get(i).isDeleted()) {
                notDeleted++;
            }
            i++;
            if (i == textElements.size()) {
                return -1;
            }
        }
        return i;
    }

    public Element findElementByOffset(int offset) {
        if (offset < 0 || offset >= textElements.size()) {
            return null;
        }
        int index = findElementIndexByOffset(offset);
        if (index >= 0 && index < textElements.size()) {
            return textElements.get(index);
        }
        return null;
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
