package com.tible.ocm.acceptance.configuration.testContainerInitializers;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String RABBITMQ_IMAGE = "rabbitmq:3.8.4-rc.3-management";
    private static final RabbitMQContainer rabbitMQContainer;

    static {
        rabbitMQContainer = new RabbitMQContainer(RABBITMQ_IMAGE)
                .withVhost("/ocm")
                .withExposedPorts(5672, 15672);

        rabbitMQContainer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        ConfigurableEnvironment configEnv = context.getEnvironment();

        TestPropertyValues values = TestPropertyValues.of(
                "spring.rabbitmq.host=" + rabbitMQContainer.getHost(),
                "spring.rabbitmq.port=" + rabbitMQContainer.getMappedPort(5672),
                "spring.rabbitmq.api-port=" + rabbitMQContainer.getMappedPort(15672),
                "spring.rabbitmq.addresses=" + rabbitMQContainer.getHost() + ":" + rabbitMQContainer.getMappedPort(5672)
        );
        values.applyTo(configEnv);
    }
}
