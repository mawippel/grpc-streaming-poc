package com.mawippel.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Import(KafkaAutoConfiguration.class)
public class KafkaConfig {

    @Bean
    public NewTopic generalTenantEventTopics(@Value("${kafka.topics.upstream.name}") String topicName) {
        return TopicBuilder.name(topicName).build();
    }

    @Bean
    public NewTopic internalTenantEventTopics(@Value("${kafka.topics.downstream.name}") String topicName) {
        return TopicBuilder.name(topicName).build();
    }

}
