package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.incandescent.woodaengserver.dto.game.PlayerMatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

//    @Transactional
//    public void matchingJoin(String playerId, double latitude, double longitude)
//            throws JsonProcessingException {
//        //방 현재 인원 체크
//        long matchingQuota = Long.parseLong(request.getMemberNumber());
//        var matchingRoomCapacity = redisService.waitingUserCountAndRedisConnectByRedis(request.getKey());
//        log.info("현재 방 입장 인원 =={}==",matchingRoomCapacity.toString());
//
//        //매칭 정원이 차지 않았을 경우
//        if (matchingRoomCapacity < matchingQuota - 1) {
//            redisService.machedEnterByRedis(request.getKey(), request);
//            var topicNameSelector
//                    = redisService.findByFirstJoinUserByRedis(request.getKey(), RequestMatching.class);
//            String topicName = topicNameSelector.getMemberEmail();
//            return new ResponseUrlInfo(request,topicName);
//        }
//
//        //매칭 정원이 찻을 경우
//        redisService.machedEnterByRedis(request.getKey(), request);
//        var resultMemberList =
//                redisService.getMatchingMemberByRedis(request.getKey(), matchingQuota, RequestMatching.class);
//        var topicName = resultMemberList.get(0).getMemberEmail();
//
//        List<Member> members = resultMemberList.stream()
//                .map(o -> memberService.responseMemberByMemberEmail(o.getMemberEmail())).toList();
//
//        String resultUrl = discordService.createChannel(resultMemberList.get(0).getGameMode(),
//                Integer.parseInt(resultMemberList.get(0).getMemberNumber())).orElseThrow(() -> new IllegalArgumentException("url을 찾을 수 없습니다."));
//
//        var resultMatching = ResultMatching.builder()
//                .gameInfo(resultMemberList.get(0).getKey())
//                .playMode(resultMemberList.get(0).getGameMode())
//                .discordUrl(resultUrl)
//                .build();
//        resultMatchingRepository.save(resultMatching);
//
//        members.stream()
//                .map(resultMember -> {
//                    MatchingLog matchingLog = new MatchingLog(resultMatching, resultMember);
//                    matchingLog.addMatchingLogToMember(resultMember);
//                    matchingLogRepository.save(matchingLog);
//                    return matchingLog;
//                });
//
//        var currentmatchingId = resultMatchingRepository.findFirstByDiscordUrl(resultUrl)
//                .orElseThrow(NotFoundMatchingException::new);
//
//        return ResponseUrlInfo.builder()
//                .matchingId(currentmatchingId.getId())
//                .member(request)
//                .topicName(topicName)
//                .url(resultUrl).build();
//    }


    public void joinLocationQueue(String playerId, double latitude, double longitude) {
        log.info("joinLocationQueue");
        // S2 Cell ID 생성
        S2CellId cellId = S2CellId.fromLatLng(S2LatLng.fromDegrees(latitude, longitude));
        Long cellIdLong = cellId.id();

        AtomicLong counter = cellIdCounterMap.computeIfAbsent(String.valueOf(cellIdLong), k -> new AtomicLong());
        long incrementValue = counter.getAndIncrement();

        String formattedIncrementValue = String.format("%02d", incrementValue);
        String gameCode = cellIdLong + formattedIncrementValue;

        setOperations.add("locationQueue:" + gameCode, playerId);
        notify();
        tryMatch(gameCode);
    }

    private synchronized void tryMatch(String gameCode) {
        while (true) {
            log.info("tryMatch");
            Set<String> locationQueue = setOperations.members("locationQueue:" + gameCode);

            if (locationQueue.size() >= 2) {
                sendMatchInfo(locationQueue, gameCode);
                setOperations.remove("locationQueue:" + gameCode, locationQueue.toArray());
                cellIdCounterMap.remove(gameCode);
                return;
            }

            try {
                log.info("wait");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for match");
                return;  // 대기 중 인터럽트 발생 시 메서드 종료
            }
        }
    }

    private void sendMatchInfo(Set<String> playerIds, String gameCode) {
        log.info("sendMatchInfo");
        List<String> playerList = new ArrayList<>(playerIds);
        Collections.shuffle(playerList);

//        List<String> teamP = playerList.subList(0, 3);
//        List<String> teamB = playerList.subList(3, 6);
        List<String> teamP = playerList.subList(0,1);
        List<String> teamB = playerList.subList(1,2);

        PlayerMatchResponse playerMatchResponse = new PlayerMatchResponse(gameCode, teamP, teamB);

        // 각 팀에게 팀 정보 및 게임 코드 전송
        messagingTemplate.convertAndSend("/game/matching", playerMatchResponse);
        log.info("matching");
    }
}
