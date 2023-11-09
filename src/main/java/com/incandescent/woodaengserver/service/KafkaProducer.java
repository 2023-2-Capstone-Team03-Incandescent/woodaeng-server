package com.incandescent.woodaengserver.service;

import com.incandescent.woodaengserver.dto.PlayerMatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {
    private final KafkaTemplate<String, PlayerMatchRequest> kafkaTemplate;

    public void send(String topic, PlayerMatchRequest playerMatchRequest) {
        log.info("topic : " + topic);
        log.info("send Message : " + playerMatchRequest.getNickname());
        kafkaTemplate.send(topic, playerMatchRequest);
    }
}
