package com.tible.ocm.services;

import com.tible.ocm.models.mongo.ImporterRule;
import com.tible.ocm.models.mongo.ImporterRuleLimitations;

import java.util.List;
import java.util.Optional;

public interface ImporterRuleService {

    List<ImporterRule> findAll();

    Optional<ImporterRule> findByFromEan(String fromEan);

    ImporterRule findByFromEanAndRvmOwnerAndRvmSerial(String fromEan, String rvmOwner, String rvmSerial);

    void delete(String id);

    ImporterRule save(ImporterRule importerRule, List<ImporterRuleLimitations> importerRuleLimitations);

    List<ImporterRule> getAllByRvmOwnerAndRvmSerial(String rvmOwner, List<String> rvmSerial);

    boolean remove(List<ImporterRule> importerRules);

    List<ImporterRule> findAllNotIn(List<ImporterRule> importerRules);
}
