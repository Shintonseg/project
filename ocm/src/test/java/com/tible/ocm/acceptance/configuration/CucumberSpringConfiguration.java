package com.tible.ocm.acceptance.configuration;

import com.tible.ocm.OcmApplication;
import com.tible.ocm.acceptance.CucumberPackageConfiguration;
import com.tible.ocm.acceptance.configuration.testContainerInitializers.ConsulTestContainerInitializer;
import com.tible.ocm.acceptance.configuration.testContainerInitializers.MongoTestContainerInitializer;
import com.tible.ocm.acceptance.configuration.testContainerInitializers.RabbitMQTestContainerInitializer;
import com.tible.ocm.acceptance.configuration.testContainerInitializers.RedisTestContainerInitializer;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;

@NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
@ActiveProfiles(profiles = {"dev"})
@CucumberContextConfiguration
@ContextConfiguration(
        classes = {OcmApplication.class},
        initializers = {MongoTestContainerInitializer.class,
                RedisTestContainerInitializer.class,
                RabbitMQTestContainerInitializer.class,
                ConsulTestContainerInitializer.class}
)
@SpringBootTest(classes = {CucumberPackageConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CucumberSpringConfiguration {
}

