package com.tible.ocm.services;

import com.tible.ocm.models.mongo.OAuthClient;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.List;
import java.util.Optional;

public interface OAuthClientService {

    Optional<OAuthClient> findByClientId(String clientId);

    List<OAuthClient> findAll();

    OAuthClient save(OAuthClient client);

    boolean remove(List<OAuthClient> clients);

    boolean checkExistClientId(OAuthClient client, String clientId);

    List<OAuthClient> getClients(List<String> clientIds);

    List<OAuthClient> findAllNotIn(List<OAuthClient> OAuthClients);

    String getVersion(OAuth2Authentication auth);
}
