package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incandescent.woodaengserver.config.KafkaId;
import com.incandescent.woodaengserver.dto.PlayerMatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final SimpMessagingTemplate template;

    @KafkaListener(topics = KafkaId.KAFKA_TOPIC, groupId = KafkaId.KAFKA_GRPUP_ID)
    public void consume(PlayerMatchRequest playerMatchRequest) throws IOException {
        log.info("Consumed Message : " + playerMatchRequest.getNickname());
        HashMap<String, String> msg = new HashMap<>() {{
            put("game_code", String.valueOf(playerMatchRequest.getGame_code()));
            put("nickname", playerMatchRequest.getNickname());
            put("latitude", String.valueOf(playerMatchRequest.getLatitude()));
            put("longitude", String.valueOf(playerMatchRequest.getLongitude()));
        }};

        ObjectMapper mapper = new ObjectMapper();
        template.convertAndSend("/topic/tt", mapper.writeValueAsString(msg));
    }
}
