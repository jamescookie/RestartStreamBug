package com.example;

import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

@Factory
@Requires(property = "enable.stream1", value = "true", defaultValue = "false")
public class Stream1Factory {

    @Singleton
    @Named("default")
    KStream<String, String> bankAccountEventKStream(@Named("default") ConfiguredStreamBuilder builder) {
        Properties props = builder.getConfiguration();
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        KStream<String, String> source = builder.stream("topic1");
        source
                .map((k, v) -> new KeyValue<>(k, v + "-stream1"))
                .to("output");

        return source;
    }
}
