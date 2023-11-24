package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incandescent.woodaengserver.dto.game.GamePlayRequest;
import com.incandescent.woodaengserver.dto.game.GamePlayResponse;
import com.incandescent.woodaengserver.dto.game.GameReadyResponse;
import com.incandescent.woodaengserver.dto.game.GameResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
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
    private final RedisPublisher redisPublisher;
    private final RedisSubscriber redisSubscriber;
    private String gameCode;
    private boolean isContainerRunning = false;

    private String startTime;
    private String endTime;

    @Autowired
    public GamePlayService(RedisMessageListenerContainer container, RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate, RedisPublisher redisPublisher, RedisSubscriber redisSubscriber) {
        this.redisTemplate = redisTemplate;
        this.container = container;
        this.messagingTemplate = messagingTemplate;
        this.redisPublisher = redisPublisher;
        this.redisSubscriber = redisSubscriber;
    }

    public synchronized void subscribeToRedis(String topic) {
        redisSubscriber.setTopic(topic);
        container.addMessageListener(redisSubscriber, new PatternTopic(topic));
    }

    public synchronized void unsubscribeFromRedis() {
        container.stop();
    }

    public void readyGame(String gameCode, Long id, int team) throws JsonProcessingException {
        this.gameCode = gameCode;
        LocalDateTime startTime = LocalDateTime.now().plusSeconds(5);
        LocalDateTime endTime = startTime.plusMinutes(15);

        this.startTime = startTime.getSecond() + " " +  startTime.getMinute() + " " +  startTime.getHour() + " " +  startTime.getDayOfMonth() + " " +  startTime.getMonth() + " " +  startTime.getDayOfWeek();
        this.endTime = endTime.getSecond() + " " +  endTime.getMinute() + " " +  endTime.getHour() + " " +  endTime.getDayOfMonth() + " " +  endTime.getMonth() + " " +  endTime.getDayOfWeek();

        ObjectMapper objectMapper = new ObjectMapper();
        GameReadyResponse gameReadyResponse = new GameReadyResponse(startTime.toString());
        String jsonGameReadyResponse = objectMapper.writeValueAsString(gameReadyResponse);
        messagingTemplate.convertAndSend("/topic/game/ready/"+gameCode, jsonGameReadyResponse);

        subscribeToRedis("/game/play/"+gameCode);
//        startGame();
    }

    public void playGame(String gameCode, GamePlayRequest gamePlayRequest) throws JsonProcessingException {
        GamePlayResponse gamePlayResponse = null;
        if (gamePlayRequest.getBallId() == 20) {
            int ball1 = (int) (Math.random() * 20);
            int ball2 = (int) (Math.random() * 20);
            while (ball1 == ball2)
                ball2 = (int) (Math.random() * 20);
            gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), ball1, ball2, 10, 10, 1);

        } else if (gamePlayRequest.getBallId() == 21) {
            int random = (int) (Math.random() * 3);
            switch (random) {
                case 0: //2개+
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), gamePlayRequest.getBallId(), 30, 10, 10, 1);
                    break;
                case 1: //2개-
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), gamePlayRequest.getBallId(), 30, 10, 10, 2);
                    break;
                case 2: //안개
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), 30, 30, 10, 10, 3);
                    break;
            }


        } else {
            gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), gamePlayRequest.getBallId(), 30, 10, 10, 0);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonGamePlayResponse = objectMapper.writeValueAsString(gamePlayResponse);
        messagingTemplate.convertAndSend("/topic/game/play/"+gameCode, jsonGamePlayResponse);
        redisPublisher.publishGameEvent(gameCode, jsonGamePlayResponse);
    }


//    @Scheduled(d) //랜덤박스

//    @Scheduled(cron = "#{@startTime}", zone =  "Asia/Seoul")
//    public void startGame() {
//        endGame();
//    }
//
//    @Async
//    @Scheduled(cron = endTime., zone =  "Asia/Seoul")
//    public void endGame() {
//        unsubscribeFromRedis();
//        GameResultResponse gameResultResponse = new GameResultResponse();
//        String jsonGameResultResponse = null;
//        try {
//            jsonGameResultResponse = new ObjectMapper().writeValueAsString(gameResultResponse);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        messagingTemplate.convertAndSend("/topic/game/end/"+gameCode, jsonGameResultResponse);
//    }
}
