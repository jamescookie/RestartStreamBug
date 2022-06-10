package com.example;

import io.micronaut.context.ApplicationContext;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.streams.KafkaStreams;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class RestartStreamBugTest {
    private static final ConditionFactory WAIT = await().atMost(5, TimeUnit.SECONDS).ignoreExceptions();
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
    private ApplicationContext applicationContext;

    @BeforeAll
    static void startKafka() {
        kafka.start();
    }

    @AfterEach
    public void after() {
        Optional.ofNullable(applicationContext).ifPresent(ApplicationContext::stop);
    }

    @Test
    void testSingleStream() {
        String message = "message1";
        createTopics("topic1");
        createContext("test1", Map.of("enable.stream1", "true"));
        sendMessage(message, Topic1Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(message + "-stream1")));
    }

    @Test
    void testTwoStreams() {
        String message = "message2";
        createTopics("topic1", "topic2");
        String applicationId = "test2";
        createContext(applicationId, Map.of(
                "enable.stream1", "true",
                "enable.stream2", "true",
                "kafka.streams.stream2.application.id", applicationId + "-stream2"));
        sendMessage(message, Topic1Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(message + "-stream1")));
        sendMessage(message, Topic2Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(message + "-stream2")));
    }

    @Test
    void testTwoStreamsRestart() {
        String messageBeforeRestart = "messageBeforeRestart";
        createTopics("topic1", "topic2");
        String applicationId = "test3";
        createContext(applicationId, Map.of(
                "enable.stream1", "true",
                "enable.stream2", "true",
                "kafka.streams.stream2.application.id", applicationId + "-stream2"));

        sendMessage(messageBeforeRestart, Topic1Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(messageBeforeRestart + "-stream1")));
        sendMessage(messageBeforeRestart, Topic2Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(messageBeforeRestart + "-stream2")));

        applicationContext.stop();

        String messageAfterRestart = "messageAfterRestart";
        createContext(applicationId, Map.of(
                "enable.stream1", "true",
                "enable.stream2", "true",
                "kafka.streams.stream2.application.id", applicationId + "-stream2"));

        sendMessage(messageAfterRestart, Topic1Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(messageAfterRestart + "-stream1")));
        sendMessage(messageAfterRestart, Topic2Producer.class);
        WAIT.untilAsserted(() -> assertTrue(receivedMessage(messageAfterRestart + "-stream2")));
    }




    private boolean receivedMessage(String message) {
        return applicationContext.getBean(OutputListener.class).received(message);
    }

    private <T extends MessageSender> void sendMessage(String message, Class<T> clazz) {
        applicationContext.getBean(clazz).send(message);
    }

    private void ensureStreamsAreRunning() {
        Collection<KafkaStreams> streams = applicationContext.getBeansOfType(KafkaStreams.class);
        await().atMost(30, TimeUnit.SECONDS).ignoreExceptions().untilAsserted(() -> assertTrue(streams.stream().allMatch(stream -> {
            boolean b = stream.state() == KafkaStreams.State.RUNNING;
            if (!b) {
                System.out.println(stream.allMetadata().toString());
            }
            return b;
        })));
    }

    private static void createTopics(String... topics) {
        var newTopics =
                Arrays.stream(topics)
                        .map(topic -> new NewTopic(topic, 1, (short) 1))
                        .collect(Collectors.toList());
        try (var admin = AdminClient.create(Map.of(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            admin.createTopics(newTopics);
        }
    }

    private void createContext(String name, Map<String, Object> extraProps) {
        applicationContext = createContext(name, extraProps, "test");
        ensureStreamsAreRunning();
    }

    private static ApplicationContext createContext(String name, Map<String, Object> extraProps, String... environments) {
        Map<String, Object> properties = new HashMap<>(Map.of(
                "micronaut.application.name", name,
                "kafka." + BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()
        ));
        properties.putAll(extraProps);
        return ApplicationContext.run(properties, environments);
    }
}
