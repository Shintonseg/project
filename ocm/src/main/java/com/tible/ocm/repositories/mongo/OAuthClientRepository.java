package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.OAuthClient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OAuthClientRepository extends MongoRepository<OAuthClient, String> {

    OAuthClient findByClientId(String clientId);

    Long countByClientIdAndIdNot(String clientId, String id);

    Long countByClientId(String clientId);

    List<OAuthClient> findAllByClientIdNotIn(List<String> oauthClients);

}
