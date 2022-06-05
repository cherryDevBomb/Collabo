package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.Heartbeat;
import com.github.cherrydevbomb.collabo.communication.subscriber.DeleteAckSubscriber;
import com.github.cherrydevbomb.collabo.communication.subscriber.HeartbeatSubscriber;
import com.github.cherrydevbomb.collabo.communication.subscriber.RemoteDocumentChangeSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.Getter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommunicationService {

    @Getter
    protected String currentSessionId;

    @Getter
    protected String userId;

    protected final StatefulRedisPubSubConnection<String, String> redisPubConnection;
    protected final StatefulRedisPubSubConnection<String, String> redisSubConnection;

    protected RedisPubSubListener<String, String> heartbeatSubscriber;
    protected RedisPubSubListener<String, String> deleteAckSubscriber;
    protected RedisPubSubListener<String, String> remoteDocumentChangeSubscriber;

    private ScheduledExecutorService heartbeatExecutor;

    public CommunicationService() {
        redisPubConnection = RedisConfig.getRedisPubConnection();
        redisSubConnection = RedisConfig.getRedisSubConnection();
    }

    protected void subscribeToHeartbeatChannel() {
        String heartbeatChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.HEARTBEAT_CHANNEL);
        heartbeatSubscriber = new HeartbeatSubscriber();
        subscribe(heartbeatChannel, heartbeatSubscriber);
    }

    protected void startHeartbeat() {
        heartbeatExecutor = Executors.newScheduledThreadPool(1);
        heartbeatExecutor.scheduleWithFixedDelay(this::sendHeartbeatMessage, 0, 3, TimeUnit.SECONDS);
    }

    protected void stopHeartbeat() {
        heartbeatExecutor.shutdown();
    }

    public void subscribeToDeleteAckChannel() {
        String deleteAckChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DELETE_ACK_CHANNEL);
        deleteAckSubscriber = new DeleteAckSubscriber();
        subscribe(deleteAckChannel, deleteAckSubscriber);
    }

    private void sendHeartbeatMessage() {
        String heartbeatChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.HEARTBEAT_CHANNEL);
        RedisPubSubAsyncCommands<String, String> asyncPub = redisPubConnection.async();
        Heartbeat heartbeat = new Heartbeat(userId, System.currentTimeMillis());
        try {
            long subscriberCount = asyncPub.publish(heartbeatChannel, heartbeat.serialize()).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void unsubscribeAll() {
        ((RemoteDocumentChangeSubscriber)remoteDocumentChangeSubscriber).stopQueueProcessing();
        String documentChangeChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DOCUMENT_CHANGE_CHANNEL);
        unsubscribe(documentChangeChannel, remoteDocumentChangeSubscriber);

        stopHeartbeat();
        String heartbeatChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.HEARTBEAT_CHANNEL);
        unsubscribe(heartbeatChannel, heartbeatSubscriber);

        String deleteAckChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.DELETE_ACK_CHANNEL);
        unsubscribe(deleteAckChannel, deleteAckSubscriber);
    }

    protected void subscribe(String channel, RedisPubSubListener<String, String> subscriber) {
        redisSubConnection.addListener(subscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(channel);
    }

    protected void unsubscribe(String channel, RedisPubSubListener<String, String> subscriber) {
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(channel);
        redisSubConnection.removeListener(subscriber);
        subscriber = null;
    }

    protected void invalidateSession() {
        currentSessionId = null;
        userId = null;
    }
}
