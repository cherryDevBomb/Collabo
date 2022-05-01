package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.DeleteAck;
import com.github.cherrydevbomb.collabo.editor.crdt.Element;
import com.github.cherrydevbomb.collabo.editor.crdt.ID;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
public class DeleteAckService {

    private static DeleteAckService deleteAckService;

    private final Map<ID, Set<String>> deleteAckMap;
    private final HeartbeatService heartbeatService;

    @Setter
    private String deleteAckChannel;

    private final StatefulRedisPubSubConnection<String, String> redisPubConnection;

    public static DeleteAckService getInstance() {
        if (deleteAckService == null) {
            deleteAckService = new DeleteAckService();
        }
        return deleteAckService;
    }

    private DeleteAckService() {
        deleteAckMap = new HashMap<>();
        heartbeatService = HeartbeatService.getInstance();
        redisPubConnection = RedisConfig.getRedisPubConnection();
    }

    public void sendDeleteAck(Element element, String userId) {
        RedisPubSubAsyncCommands<String, String> async = redisPubConnection.async();
        DeleteAck deleteAckMessage = new DeleteAck(element.getId(), userId);
        try {
            long subscriberCount = async.publish(deleteAckChannel, deleteAckMessage.serialize()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not publish to channel {}", deleteAckChannel);
        }
    }

    public void addDeleteAck(DeleteAck deleteAck) {
        if (!deleteAckMap.containsKey(deleteAck.getElementId())) {
            deleteAckMap.put(deleteAck.getElementId(), new HashSet<>());
        }
        deleteAckMap.get(deleteAck.getElementId()).add(deleteAck.getUserId());
    }

    public boolean allAcksReceived(ID elementId) {
        long connectedPeers = heartbeatService.getConnectedPeersCount();
        long deleteAcksReceived = deleteAckMap.get(elementId).size();

        return connectedPeers == deleteAcksReceived;
    }

    public void removeElementFromAckMap(ID elementId) {
        deleteAckMap.remove(elementId);
    }
}
