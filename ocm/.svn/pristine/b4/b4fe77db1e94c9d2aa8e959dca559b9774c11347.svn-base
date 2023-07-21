package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.repositories.mongo.OAuthClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

@Service
public class MongoClientDetailsService implements ClientDetailsService {

    @Autowired
    private OAuthClientRepository oauthClientRepository;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {

        OAuthClient client = oauthClientRepository.findByClientId(clientId);

        BaseClientDetails base = new BaseClientDetails(
                client.getClientId(), client.getResourceIds(),
                client.getScope(), client.getAuthorizedGrantTypes(), client.getAuthorities());
        base.setClientSecret(client.getClientSecret());
        base.setAccessTokenValiditySeconds(client.getRefreshTokenValidity());
        base.setRefreshTokenValiditySeconds(client.getRefreshTokenValidity());
        // base.setAdditionalInformation(client.getAdditionalInformation());
        // base.setAutoApproveScopes(client.getAutoApprove());
        return base;
    }
}
