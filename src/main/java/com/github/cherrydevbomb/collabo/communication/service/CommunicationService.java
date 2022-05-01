package com.github.cherrydevbomb.collabo.communication.service;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.Heartbeat;
import com.github.cherrydevbomb.collabo.communication.subscriber.HeartbeatSubscriber;
import com.github.cherrydevbomb.collabo.communication.util.ChannelType;
import com.github.cherrydevbomb.collabo.communication.util.ChannelUtil;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.Getter;

import java.time.LocalDateTime;
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

    RedisPubSubListener<String, String> heartbeatSubscriber;

    private ScheduledExecutorService heartbeatExecutor;

    public CommunicationService() {
        redisPubConnection = RedisConfig.getRedisPubConnection();
        redisSubConnection = RedisConfig.getRedisSubConnection();
    }

    protected void subscribeToHeartbeatChannel() {
        String heartbeatChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.HEARTBEAT_CHANNEL);
        heartbeatSubscriber = new HeartbeatSubscriber();
        redisSubConnection.addListener(heartbeatSubscriber);
        RedisPubSubAsyncCommands<String, String> asyncSub = redisSubConnection.async();
        asyncSub.subscribe(heartbeatChannel);
    }

    protected void startHeartbeat() {
        heartbeatExecutor = Executors.newScheduledThreadPool(1);
        heartbeatExecutor.scheduleWithFixedDelay(this::sendHeartbeatMessage, 0, 3, TimeUnit.SECONDS);
    }

    protected void unsubscribeHeartbeat() {
        String heartbeatChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.HEARTBEAT_CHANNEL);
        RedisPubSubAsyncCommands<String, String> async = redisSubConnection.async();
        async.unsubscribe(heartbeatChannel);
        redisSubConnection.removeListener(heartbeatSubscriber);
        heartbeatSubscriber = null;
    }

    protected void stopHeartbeat() {
        heartbeatExecutor.shutdown();
    }

    protected void invalidateSession() {
        currentSessionId = null;
        userId = null;
    }

    private void sendHeartbeatMessage() {
        System.out.println("HEARTBEAT");
        String heartbeatChannel = ChannelUtil.getChannel(currentSessionId, ChannelType.HEARTBEAT_CHANNEL);
        RedisPubSubAsyncCommands<String, String> asyncPub = redisPubConnection.async();
        Heartbeat heartbeat = new Heartbeat(userId, System.currentTimeMillis());
        try {
            long subscriberCount = asyncPub.publish(heartbeatChannel, heartbeat.serialize()).get();
            System.out.println(subscriberCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}