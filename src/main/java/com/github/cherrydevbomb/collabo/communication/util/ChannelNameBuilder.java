package com.github.cherrydevbomb.collabo.communication.util;

public class ChannelNameBuilder {

    private static final String INIT_STATE_REQUEST_CHANNEL = "_INIT_STATE_REQUEST_CHANNEL";
    private static final String INIT_STATE_TRANSFER_CHANNEL = "_INIT_STATE_TRANSFER_CHANNEL";

    public static String getInitStateRequestChannel(String sessionId) {
        return sessionId + INIT_STATE_REQUEST_CHANNEL;
    }
    
    public static String getInitStateTransferChannel(String sessionId) {
        return sessionId + INIT_STATE_TRANSFER_CHANNEL;
    }
}
