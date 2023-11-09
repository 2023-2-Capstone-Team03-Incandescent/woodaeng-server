package com.incandescent.woodaengserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 메세지브로커 등록 (topic: 1:N / queue: 1:1)
        registry.setApplicationDestinationPrefixes("/kafka"); // 경로 설정 (실제 경로: /kafka/topic/###)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/game") // 엔드포인트 지정 (localhost:8080/ws-chat)
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
