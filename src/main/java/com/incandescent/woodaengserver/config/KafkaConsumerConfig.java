package com.incandescent.woodaengserver.config;

import com.incandescent.woodaengserver.dto.PlayerMatchRequest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PlayerMatchRequest> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PlayerMatchRequest> factory = new ConcurrentKafkaListenerContainerFactory<String, PlayerMatchRequest>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
    @Bean
    public ConsumerFactory<String, PlayerMatchRequest> consumerFactory() {
        JsonDeserializer<PlayerMatchRequest> deserializer = new JsonDeserializer<PlayerMatchRequest>(PlayerMatchRequest.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        HashMap<String, Object> config = new HashMap<String, Object>() {{
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaId.KAFKA_BROKER);
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            put("group.id", KafkaId.KAFKA_GRPUP_ID);
        }};

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }
    //나는 MessageDto라는 객체로 Json을 받아들일 건데, 이게 내가 생성한 임의의 객체라서 카프카에서 해독을 할 수 있게 도와줘야 한다.
}
