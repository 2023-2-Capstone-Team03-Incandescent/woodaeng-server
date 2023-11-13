package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incandescent.woodaengserver.dto.game.GamePlayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class GameSubscriber implements MessageListener {
    private final SimpMessagingTemplate messagingTemplate;
    private String topic;

    @Autowired
    public GameSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String redisMessage = new String(message.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        GamePlayRequest gamePlayRequest = null;
        try {
            gamePlayRequest = objectMapper.readValue(redisMessage, GamePlayRequest.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        messagingTemplate.convertAndSend("/topic"+topic, gamePlayRequest);
    }
}