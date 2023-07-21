package com.tible.ocm.configurations;

import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.services.OAuthClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ApplicationStartupListenerImpl implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${tible-user.username}")
    private String tibleUsername;
    @Value("${tible-user.password}")
    private String tiblePassword;
    @Value("${tible-user.scope}")
    private String tibleScope;

    @Value("${tible-admin-user.username}")
    private String tibleAdminUsername;
    @Value("${tible-admin-user.password}")
    private String tibleAdminPassword;
    @Value("${tible-admin-user.scope}")
    private String tibleAdminScope;

    @Value("${lamson-user.username}")
    private String lamsonUsername;
    @Value("${lamson-user.password}")
    private String lamsonPassword;
    @Value("${lamson-user.scope}")
    private String lamsonScope;
    @Value("${lamson-user.rvm-owner-number}")
    private String lamsonRvmOwnerNumber;

    @Value("${aldi-user.username}")
    private String aldiUsername;
    @Value("${aldi-user.password}")
    private String aldiPassword;
    @Value("${aldi-user.scope}")
    private String aldiScope;
    @Value("${aldi-user.rvm-owner-number}")
    private String aldiRvmOwnerNumber;

    @Autowired
    private OAuthClientService oauthClientService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        createOAuthClientIfNotExists(tibleUsername, tiblePassword, tibleScope, null);
        createOAuthClientIfNotExists(tibleAdminUsername, tibleAdminPassword, tibleAdminScope, null);
        createOAuthClientIfNotExists(lamsonUsername, lamsonPassword, lamsonScope, lamsonRvmOwnerNumber);
        createOAuthClientIfNotExists(aldiUsername, aldiPassword, aldiScope, aldiRvmOwnerNumber);
    }

    private void createOAuthClientIfNotExists(String username, String password, String scope, String rvmOwnerNumber) {
        Optional<OAuthClient> oauthClient = oauthClientService.findByClientId(username);

        if (oauthClient.isPresent()) {
            return;
        }

        OAuthClient newOAuthClient = new OAuthClient();
        newOAuthClient.setClientId(username);
        newOAuthClient.setClientSecret(password);
        newOAuthClient.setScope(scope);
        if (rvmOwnerNumber != null) {
            newOAuthClient.setRvmOwnerNumber(rvmOwnerNumber);
        }
        newOAuthClient.setAuthorizedGrantTypes("password,refresh_token,client_credentials");

        OAuthClient savedOAuthClient = oauthClientService.save(newOAuthClient);
        if (savedOAuthClient != null) {
            log.info("Saved oAuthClient {}", savedOAuthClient.getClientId());
        }
    }
}
