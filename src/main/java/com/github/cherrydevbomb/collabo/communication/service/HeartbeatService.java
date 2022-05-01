package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.model.Heartbeat;

import java.util.HashMap;
import java.util.Map;

public class HeartbeatService {
    
    private static HeartbeatService heartbeatService;

    private Map<String, Heartbeat> heartbeatMap = new HashMap<>();

    public static HeartbeatService getInstance() {
        if (heartbeatService == null) {
            heartbeatService = new HeartbeatService();
        }
        return heartbeatService;
    }

    public void addHeartbeat(Heartbeat heartbeat) {
        heartbeatMap.put(heartbeat.getUserId(), heartbeat);
    }
}
