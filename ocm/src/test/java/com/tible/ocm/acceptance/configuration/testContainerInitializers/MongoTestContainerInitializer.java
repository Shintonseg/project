package com.tible.ocm.acceptance.configuration.testContainerInitializers;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public class MongoTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5")).withReuse(true);  //todo check

        mongoDBContainer.start();
    }

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        ConfigurableEnvironment ce = context.getEnvironment();

        TestPropertyValues values = TestPropertyValues.of(
                "spring.data.mongodb.uri=" + mongoDBContainer.getReplicaSetUrl()
        );
        values.applyTo(ce);
    }
}
