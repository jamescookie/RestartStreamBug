package com.example;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

import java.util.UUID;

@KafkaClient
interface Topic1Producer extends MessageSender {
    @Topic("topic1")
    void send(@KafkaKey String key, String message);

    default void send(String message) {
        send(UUID.randomUUID().toString(), message);
    }
}
