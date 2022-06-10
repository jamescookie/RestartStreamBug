package com.example;

import io.micronaut.configuration.kafka.annotation.*;
import io.micronaut.context.annotation.Property;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.HashMap;
import java.util.Map;

@KafkaListener(
        offsetReset = OffsetReset.EARLIEST,
        offsetStrategy = OffsetStrategy.DISABLED,
        groupId = "test", uniqueGroupId = true,
        properties = {
                @Property(name = ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, value = "org.apache.kafka.common.serialization.StringDeserializer"),
                @Property(name = ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, value = "org.apache.kafka.common.serialization.StringDeserializer")
        }
)
public class OutputListener {
    private final Map<String, String> messages = new HashMap<>();

    @Topic("output")
    public void receiveMessage(@KafkaKey String key, String message) {
        messages.put(key, message);
    }

    public boolean received(String message) {
        return messages.values().stream().anyMatch(s -> s.equals(message));
    }

    public void clear() {
        messages.clear();
    }
}
