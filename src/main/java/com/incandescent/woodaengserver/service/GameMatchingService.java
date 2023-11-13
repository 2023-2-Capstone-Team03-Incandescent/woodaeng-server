package com.incandescent.woodaengserver.service;

import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.incandescent.woodaengserver.dto.game.PlayerMatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class GameMatchingService {
    private final RedisTemplate<String, String> redisTemplate;
    private final SetOperations<String, String> setOperations;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, AtomicLong> cellIdCounterMap = new ConcurrentHashMap<>();

    @Autowired
    public GameMatchingService(RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.setOperations = redisTemplate.opsForSet();
        this.messagingTemplate = messagingTemplate;
    }

    public void joinLocationQueue(String playerId, double latitude, double longitude) {
        // S2 Cell ID 생성
        S2CellId cellId = S2CellId.fromLatLng(S2LatLng.fromDegrees(latitude, longitude));
        Long cellIdLong = cellId.id();

        AtomicLong counter = cellIdCounterMap.computeIfAbsent(String.valueOf(cellIdLong), k -> new AtomicLong());
        long incrementValue = counter.getAndIncrement();

        String formattedIncrementValue = String.format("%02d", incrementValue);
        String gameCode = cellIdLong + formattedIncrementValue;

        setOperations.add("locationQueue:" + gameCode, playerId);
        tryMatch(gameCode);
    }

    private void tryMatch(String gameCode) {
        Set<String> locationQueue = setOperations.members("locationQueue:" + gameCode);

        if (locationQueue.size() >= 6) {
            sendMatchInfo(locationQueue, gameCode);
            setOperations.remove("locationQueue:" + gameCode, locationQueue.toArray());
            cellIdCounterMap.remove(gameCode);
        }
    }

    private void sendMatchInfo(Set<String> playerIds, String gameCode) {
        List<String> playerList = new ArrayList<>(playerIds);
        Collections.shuffle(playerList);

        List<String> teamP = playerList.subList(0, 3);
        List<String> teamB = playerList.subList(3, 6);

        PlayerMatchResponse playerMatchResponse = new PlayerMatchResponse(gameCode, teamP, teamB);

        // 각 팀에게 팀 정보 및 게임 코드 전송
        messagingTemplate.convertAndSend("/game/matching", playerMatchResponse);
        log.info("matching");
    }
}
