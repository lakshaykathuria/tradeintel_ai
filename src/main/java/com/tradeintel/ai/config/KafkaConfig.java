package com.tradeintel.ai.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.topics.market-data}")
    private String marketDataTopic;

    @Value("${spring.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${spring.kafka.topics.signal-events}")
    private String signalEventsTopic;

    @Value("${spring.kafka.topics.portfolio-updates}")
    private String portfolioUpdatesTopic;

    @Bean
    public NewTopic marketDataTopic() {
        return TopicBuilder.name(marketDataTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(orderEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic signalEventsTopic() {
        return TopicBuilder.name(signalEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic portfolioUpdatesTopic() {
        return TopicBuilder.name(portfolioUpdatesTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
