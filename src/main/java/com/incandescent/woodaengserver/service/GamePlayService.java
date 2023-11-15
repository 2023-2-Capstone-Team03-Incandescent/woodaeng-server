package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incandescent.woodaengserver.dto.game.GamePlayRequest;
import com.incandescent.woodaengserver.dto.game.GameReadyResponse;
import com.incandescent.woodaengserver.dto.game.GameResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.listener.PatternTopic;

import java.time.LocalDateTime;

@Slf4j
@Service
public class GamePlayService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisMessageListenerContainer container;
    private final SimpMessagingTemplate messagingTemplate;
    private final GamePublisher gamePublisher;
    private final GameSubscriber gameSubscriber;
    private String gameCode;
    private boolean isContainerRunning = false;

    @Autowired
    public GamePlayService(RedisMessageListenerContainer container, RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate, GamePublisher gamePublisher, GameSubscriber gameSubscriber) {
        this.redisTemplate = redisTemplate;
        this.container = container;
        this.messagingTemplate = messagingTemplate;
        this.gamePublisher = gamePublisher;
        this.gameSubscriber = gameSubscriber;
    }

    public synchronized void subscribeToRedis(String topic) {
        gameSubscriber.setTopic(topic);
        container.addMessageListener(gameSubscriber, new PatternTopic(topic));

//        if (!isContainerRunning) {
//            gameSubscriber.setTopic(topic);
//            container.addMessageListener(new MessageListenerAdapter(gameSubscriber, "onMessage"), new PatternTopic(topic));
////            container.setConnectionFactory(redisTemplate.getConnectionFactory());
//            container.afterPropertiesSet();
//            container.start();
//            isContainerRunning = true;
//        }
    }

    public synchronized void unsubscribeFromRedis() {
        container.stop();
    }

    public void readyGame(String gameCode, String id, int team) {
        this.gameCode = gameCode;
        LocalDateTime startTime = LocalDateTime.now().plusSeconds(5);



        ObjectMapper objectMapper = new ObjectMapper();
        GameReadyResponse gameReadyResponse = new GameReadyResponse(startTime.toString());
        String jsonGameReadyResponse = null;
        try {
            jsonGameReadyResponse = objectMapper.writeValueAsString(gameReadyResponse);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        messagingTemplate.convertAndSend("/topic/game/ready/"+gameCode, JsonEncoding.valueOf(gameReadyResponse.toString()));

        subscribeToRedis("/game/play/"+gameCode);
        startGame();
    }

    public void playGame(String gameCode, GamePlayRequest gamePlayRequest) {
        String jsonString = null;
        try {
            jsonString = new ObjectMapper().writeValueAsString(gamePlayRequest);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        messagingTemplate.convertAndSend("/topic/game/play/"+gameCode, JsonEncoding.valueOf(gamePlayRequest.toString()));
        gamePublisher.publishGameEvent(gameCode, jsonString);
    }


    @Scheduled(fixedDelay = 5000)
    public void startGame() {
        endGame();
    }

    @Async
    @Scheduled(fixedDelay  = 900000) // 15분(900,000밀리초)
    public void endGame() {
        unsubscribeFromRedis();
        GameResultResponse gameResultResponse = new GameResultResponse();
        String jsonGameResultResponse = null;
        try {
            jsonGameResultResponse = new ObjectMapper().writeValueAsString(gameResultResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        messagingTemplate.convertAndSend("/topic/game/end/"+gameCode, jsonGameResultResponse);
    }
}
