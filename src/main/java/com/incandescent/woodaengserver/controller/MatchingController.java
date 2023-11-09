package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.config.KafkaId;
import com.incandescent.woodaengserver.dto.PlayerMatchRequest;
import com.incandescent.woodaengserver.repository.PlayerGameLogRepository;
import com.incandescent.woodaengserver.service.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MatchingController {
//    private final ChatServiceImpl chatService;
    private final KafkaTemplate<String, PlayerMatchRequest> kafkaTemplate;
    private final KafkaProducer kafkaProducer;
    private final PlayerGameLogRepository playerGameLogRepository;

    @PostMapping("/publish")
    public void sendMessage(@RequestBody PlayerMatchRequest playerMatchRequest) {
        log.info("ChatController -> sendMessage : " + playerMatchRequest.getNickname());
        try {
            kafkaTemplate.send(KafkaId.KAFKA_TOPIC, playerMatchRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/message")
    @MessageMapping("/message")
    public void message(@RequestBody PlayerMatchRequest playerMatchRequest) {
        log.info(playerMatchRequest.getNickname());
        playerGameLogRepository.save(playerMatchRequest);
        kafkaProducer.send(KafkaId.KAFKA_TOPIC, playerMatchRequest);
    }

    @PostMapping("/history")
    public List<PlayerMatchRequest> getMessageHistory() {
        log.info("history 호출");
        return playerGameLogRepository.get();
    }

    // 클라이언트 창이 따로없어, Stomp Websocket을 연결하는 것을 확인할 수 없어 PostMapping을 추가해주었다.
    // kafka/message 로 메세지를 날리면, 프로듀서 서비스를 호출하여 날려버린다.
    // /publish 는 위작업을 컨트롤러에서 바로 처리하는 작업인 것이다.


//    @MessageMapping("/sendMessage")
//    @SendTo("/topic/public")
//    public String broadcastMessage(String message) {
//        return message;
//    }
}