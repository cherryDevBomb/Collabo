package com.github.cherrydevbomb.collabo.communication.subscriber;

import com.github.cherrydevbomb.collabo.communication.config.RedisConfig;
import com.github.cherrydevbomb.collabo.communication.model.InitialState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class HostInitStateRequestSubscriber implements RedisPubSubListener<String, String> {

    private final StatefulRedisPubSubConnection<String, String> redisConnection;
    private final String initStateTransferChannel;
    private final VirtualFile virtualFile;

    public HostInitStateRequestSubscriber(String initStateTransferChannel, VirtualFile virtualFile) {
        this.redisConnection = RedisConfig.getRedisPubConnection();
        this.initStateTransferChannel = initStateTransferChannel;
        this.virtualFile = virtualFile;
    }

    @Override
    public void message(String channel, String message) {
        System.out.println("Host received: " + message);

        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(virtualFile));
        InitialState initialState = InitialState.builder()
                .text(document.getText())
                .fileName(virtualFile.getName())
                .build();

        RedisPubSubAsyncCommands<String, String> async = redisConnection.async();
        try {
            long subscriberCount = async.publish(initStateTransferChannel, initialState.toString()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not publish to channel {}", initStateTransferChannel);
        }
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