package com.tible.ocm.acceptance.configuration.testContainerInitializers;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.consul.ConsulContainer;

public class ConsulTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final ConsulContainer consulContainer;

    static {
        consulContainer = new ConsulContainer("consul:latest")
                .withExposedPorts(8500)
                .withConsulCommand("kv put config/testing1 value123");

        consulContainer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        ConfigurableEnvironment configEnv = context.getEnvironment();

        TestPropertyValues values = TestPropertyValues.of(
                "consul.host=" + consulContainer.getHost(),
                "consul.port=" + consulContainer.getMappedPort(8500)
        );
        values.applyTo(configEnv);
    }
}
