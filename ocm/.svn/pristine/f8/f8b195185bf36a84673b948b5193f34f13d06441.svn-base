package com.tible.ocm.acceptance.configuration.testContainerInitializers;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer<?> genericContainer;

    static {
        genericContainer = new GenericContainer<>(DockerImageName.parse("redis:alpine"))
                .withExposedPorts(6379)
                .withCommand("redis-server --requirepass redis");
        genericContainer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        ConfigurableEnvironment configEnv = context.getEnvironment();

        TestPropertyValues values = TestPropertyValues.of(
                "spring.redis.host=" + genericContainer.getHost(),
                "spring.redis.port=" + genericContainer.getMappedPort(6379)
        );
        values.applyTo(configEnv);
    }
}

