package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.model.Heartbeat;

import java.util.HashMap;
import java.util.Map;

public class HeartbeatService {
    
    private static HeartbeatService heartbeatService;

    private final Map<String, Heartbeat> heartbeatMap;

    private static final int TIMEOUT_IN_MILLISECONDS = 5000;

    public static HeartbeatService getInstance() {
        if (heartbeatService == null) {
            heartbeatService = new HeartbeatService();
        }
        return heartbeatService;
    }

    private HeartbeatService() {
        heartbeatMap = new HashMap<>();
    }

    public void addHeartbeat(Heartbeat heartbeat) {
        heartbeatMap.put(heartbeat.getUserId(), heartbeat);
    }

    public long getConnectedPeersCount() {
        return heartbeatMap.values().stream()
                .filter(lastHeartbeat -> (System.currentTimeMillis() - lastHeartbeat.getTimestamp()) < TIMEOUT_IN_MILLISECONDS)
                .count();
    }
}
