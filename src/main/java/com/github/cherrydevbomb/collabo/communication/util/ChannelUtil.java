package com.github.cherrydevbomb.collabo.communication.util;

public class ChannelUtil {

    // TODO modify to use enum
    public static final String INIT_STATE_REQUEST_CHANNEL = "_INIT_STATE_REQUEST_CHANNEL";
    public static final String INIT_STATE_TRANSFER_CHANNEL = "_INIT_STATE_TRANSFER_CHANNEL";
    public static final String DOCUMENT_CHANGE_CHANNEL = "_DOCUMENT_CHANGE_CHANNEL";

    public static String getInitStateRequestChannel(String sessionId) {
        return sessionId + INIT_STATE_REQUEST_CHANNEL;
    }
    
    public static String getInitStateTransferChannel(String sessionId) {
        return sessionId + INIT_STATE_TRANSFER_CHANNEL;
    }

    public static String getDocumentChangeChannel(String sessionId) {
        return sessionId + DOCUMENT_CHANGE_CHANNEL;
    }
}
