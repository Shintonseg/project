package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.ImporterRuleLimitations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImporterRuleLimitationsRepository extends MongoRepository<ImporterRuleLimitations, String> {

    List<ImporterRuleLimitations> findAllByRvmOwner(String rvmOwner);

    List<ImporterRuleLimitations> findAllByImporterRuleId(String importerRuleId);
}
