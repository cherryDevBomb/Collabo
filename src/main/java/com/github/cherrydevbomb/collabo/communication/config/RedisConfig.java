package com.github.cherrydevbomb.collabo.communication.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public class RedisConfig {

    private static final String host = System.getenv("HOST");
    private static final int port = Integer.parseInt(System.getenv("PORT"));
    //    private final String user = System.getenv("USER");
    private static final String password = System.getenv("PASSWORD");

    private static StatefulRedisPubSubConnection<String, String> redisPubConnection;
    private static StatefulRedisPubSubConnection<String, String> redisSubConnection;

    public static StatefulRedisPubSubConnection<String, String> getRedisPubConnection() {
        if (redisPubConnection == null) {
            redisPubConnection = initRedisConnection();
        }
        return redisPubConnection;
    }

    public static StatefulRedisPubSubConnection<String, String> getRedisSubConnection() {
        if (redisSubConnection == null) {
            redisSubConnection = initRedisConnection();
        }
        return redisSubConnection;
    }

    private static StatefulRedisPubSubConnection<String, String> initRedisConnection() {
        RedisURI redisURI = RedisURI.Builder
                .redis(host, port)
//                .withPassword(new StringBuilder("password"))
//                .withDatabase(1)
                .build();

        RedisClient redisClient = RedisClient.create(redisURI);
        return redisClient.connectPubSub();
    }
}