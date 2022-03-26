package com.github.cherrydevbomb.collabo.communication.subscriber;

import io.lettuce.core.pubsub.RedisPubSubListener;

public class PeerInitStateTransferSubscriber implements RedisPubSubListener<String, String> {

    @Override
    public void message(String channel, String message) {
        System.out.println("Peer received initial state: " + message);
    }

    @Override
    public void message(String pattern, String channel, String message) {

    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }
}
