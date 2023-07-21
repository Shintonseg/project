package com.tible.ocm.services.impl;

import com.google.common.base.Strings;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.repositories.mongo.OAuthClientRepository;
import com.tible.ocm.repositories.mongo.RvmMachineRepository;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.OAuthClientService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OAuthClientServiceImpl implements OAuthClientService {

    private final OAuthClientRepository oAuthClientRepository;
    private final RvmMachineRepository rvmMachineRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyService companyService;

    public OAuthClientServiceImpl(OAuthClientRepository oAuthClientRepository,
                                  RvmMachineRepository rvmMachineRepository,
                                  PasswordEncoder passwordEncoder,
                                  CompanyService companyService) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.rvmMachineRepository = rvmMachineRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyService = companyService;
    }

    @Override
    public Optional<OAuthClient> findByClientId(String clientId) {
        return Optional.ofNullable(oAuthClientRepository.findByClientId(clientId));
    }

    @Override
    public List<OAuthClient> findAll() {
        return oAuthClientRepository.findAll();
    }

    @Override
    public OAuthClient save(OAuthClient oauthClient) {
        if (Strings.isNullOrEmpty(oauthClient.getClientId())) {
            throw new RuntimeException("Specify client ID.");
        }
        if (Strings.isNullOrEmpty(oauthClient.getClientSecret())) {
            throw new RuntimeException("Specify Client Secret.");
        }

        oauthClient.setClientSecret(passwordEncoder.encode(oauthClient.getClientSecret()));

        OAuthClient savedClient = oAuthClientRepository.findByClientId(oauthClient.getClientId());
        return oAuthClientRepository.save(savedClient == null ? oauthClient : fillClient(savedClient, oauthClient));
    }

    @Override
    public List<OAuthClient> findAllNotIn(List<OAuthClient> oauthClients) {
        return oAuthClientRepository.findAllByClientIdNotIn(oauthClients.stream()
                .map(OAuthClient::getClientId)
                .collect(Collectors.toList()));
    }

    private OAuthClient fillClient(OAuthClient savedClient, OAuthClient oauthClient) {
        savedClient.setClientSecret(oauthClient.getClientSecret());
        savedClient.setScope(oauthClient.getScope());
        savedClient.setResourceIds(oauthClient.getResourceIds());
        savedClient.setAuthorizedGrantTypes(oauthClient.getAuthorizedGrantTypes());
        savedClient.setAccessTokenValidity(oauthClient.getAccessTokenValidity());
        savedClient.setRefreshTokenValidity(oauthClient.getRefreshTokenValidity());
        savedClient.setVersion(oauthClient.getVersion());

        if (!reactor.util.CollectionUtils.isEmpty(oauthClient.getRvmMachines())) {
            rvmMachineRepository.saveAll(oauthClient.getRvmMachines());
        }
        return savedClient;
    }

    @Override
    public boolean remove(List<OAuthClient> clients) {
        clients.forEach(oAuthClientRepository::delete);
        return true;
    }

    @Override
    public boolean checkExistClientId(OAuthClient client, String clientId) {
        return client == null ? oAuthClientRepository.countByClientId(clientId) > 0 :
                oAuthClientRepository.countByClientIdAndIdNot(clientId, client.getId()) > 0;
    }

    @Override
    public List<OAuthClient> getClients(List<String> clientIds) {
        return CollectionUtils.isEmpty(clientIds) ?
                Collections.emptyList() :
                clientIds.stream().map(oAuthClientRepository::findByClientId).collect(Collectors.toList());
    }

    @Override
    public String getVersion(OAuth2Authentication auth) {
        String clientId = auth.getOAuth2Request().getClientId();
        if (clientId != null) {
            Company company = companyService.findFirstByIpAddress(clientId);
            if (company != null && company.getVersion() != null) {
                return company.getVersion();
            }

            Optional<OAuthClient> oAuthClientOptional = findByClientId(clientId);
            if (oAuthClientOptional.map(OAuthClient::getVersion).isPresent()) {
                return oAuthClientOptional.map(OAuthClient::getVersion).get();
            }
        }

        return null;
    }

}
