package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incandescent.woodaengserver.dto.game.GamePlayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GamePublisher {
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public GamePublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishGameEvent(String gameCode, String jsonString) {


        redisTemplate.convertAndSend("/game/play/"+gameCode, jsonString);
    }
}