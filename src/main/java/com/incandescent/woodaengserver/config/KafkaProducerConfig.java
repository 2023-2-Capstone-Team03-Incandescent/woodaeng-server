package com.incandescent.woodaengserver.config;

import com.incandescent.woodaengserver.dto.PlayerMatchRequest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, PlayerMatchRequest> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProducerConfiguration());
    }

    @Bean
    public Map<String, Object> kafkaProducerConfiguration() {
        return new HashMap<String, Object>() {{
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaId.KAFKA_BROKER);
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            put("group.id", KafkaId.KAFKA_GRPUP_ID);
        }};
    }
    //ProducerConfig.BOOTSTRAP_SERVERS_CONFIG : 브로커 주소를 설정해준다.
    //ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG : 키를 어떤 Serializer를 사용해서 설정할 것인가
    //ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG : Value는?
    //"group.id" : group id 지정
    //나는 Key는 문자열 값인 uuid이고,
    //값은 채팅 내역이 json이므로 json으로 설정해 주었다.
    // 그래서 이것을 바탕으로 ProducerFactory를 생성하고,
    // 이것을 Kafka에서 KafkaTemplate으로 활용하여 사용하는 듯 하다.

    @Bean
    public KafkaTemplate<String, PlayerMatchRequest> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
