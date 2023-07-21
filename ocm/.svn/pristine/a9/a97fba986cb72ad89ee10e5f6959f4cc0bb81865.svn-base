package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.ImporterRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImporterRuleRepository extends MongoRepository<ImporterRule, String> {

    boolean existsById(String id);

    Optional<ImporterRule> findByFromEan(String fromEan);

    List<ImporterRule> findAllByIdNotIn(List<String> ids);
}
