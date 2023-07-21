package com.tible.ocm.services.impl;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentService {

    private final Environment environment;

    public EnvironmentService(Environment environment) {
        this.environment = environment;
    }

    public boolean matchGivenProfiles(String... profiles) {
        return environment.acceptsProfiles(Profiles.of(profiles));
    }
}
