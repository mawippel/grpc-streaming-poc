package com.mawippel.producers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaEventProducer implements EventProducer {

    private final KafkaTemplate<String, UpstreamEventMessage> kafkaTemplate;
    private final String upstreamTopicName;

    public KafkaEventProducer(@Value("${kafka.topics.upstream.name}") String upstreamTopicName,
                              KafkaTemplate<String, UpstreamEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.upstreamTopicName = upstreamTopicName;
    }

    public void sendUpstreamEvent(UpstreamEventMessage message) {
        log.debug("Sending upstream event {}", message);
        kafkaTemplate.send(upstreamTopicName, message.message(), message);
    }
}
