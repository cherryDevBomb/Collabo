package com.github.cherrydevbomb.collabo.communication.util;

public class ChannelUtil {

    public static String getChannel(String sessionId, ChannelType channelType) {
        return sessionId + "_" + channelType.toString();
    }

    public static boolean isOfType(String channel, ChannelType channelType) {
        return channel.contains(channelType.toString());
    }
}
