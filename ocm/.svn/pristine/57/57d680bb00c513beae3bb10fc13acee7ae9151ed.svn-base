package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.ImporterRule;
import com.tible.ocm.models.mongo.ImporterRuleLimitations;
import com.tible.ocm.repositories.mongo.ImporterRuleLimitationsRepository;
import com.tible.ocm.repositories.mongo.ImporterRuleRepository;
import com.tible.ocm.services.ImporterRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
public class ImporterRuleServiceImpl implements ImporterRuleService {

    private final ImporterRuleRepository importerRuleRepository;
    private final ImporterRuleLimitationsRepository importerRuleLimitationsRepository;

    public ImporterRuleServiceImpl(ImporterRuleRepository importerRuleRepository,
                                   ImporterRuleLimitationsRepository importerRuleLimitationsRepository) {
        this.importerRuleRepository = importerRuleRepository;
        this.importerRuleLimitationsRepository = importerRuleLimitationsRepository;
    }

    @Override
    public List<ImporterRule> findAll() {
        return importerRuleRepository.findAll();
    }

    @Override
    public ImporterRule save(ImporterRule importerRule, List<ImporterRuleLimitations> importerRuleLimitations) {
        ImporterRule savedImporterRule = importerRuleRepository.save(importerRule);

        importerRuleLimitationsRepository
                .deleteAll(importerRuleLimitationsRepository.findAllByImporterRuleId(importerRule.getId()));

        importerRuleLimitations.forEach(limitation -> {
            limitation.setImporterRuleId(savedImporterRule.getId());
            importerRuleLimitationsRepository.save(limitation);
        });

        return savedImporterRule;
    }

    @Override
    public Optional<ImporterRule> findByFromEan(String fromEan) {
        return importerRuleRepository.findByFromEan(fromEan);
    }

    @Override
    public ImporterRule findByFromEanAndRvmOwnerAndRvmSerial(String fromEan, String rvmOwner, String rvmSerial) {
        Optional<ImporterRule> importerRuleOptional = importerRuleRepository.findByFromEan(fromEan);

        if (importerRuleOptional.isEmpty()) {
            return null;
        }

        ImporterRule importerRule = importerRuleOptional.get();
        List<ImporterRuleLimitations> importerRuleLimitationsList = importerRuleLimitationsRepository.findAllByImporterRuleId(importerRule.getId());
        Optional<ImporterRuleLimitations> importerRuleLimitationsOptional = importerRuleLimitationsList.stream()
                .filter(importerRuleLimitation -> importerRuleLimitation.getRvmOwner().equals(rvmOwner)).findFirst();
        if (importerRuleLimitationsOptional.isEmpty()) {
            return null;
        } else {
            ImporterRuleLimitations importerRuleLimitations =  importerRuleLimitationsOptional.get();
            if (importerRuleLimitations.getRvmSerials() != null && !importerRuleLimitations.getRvmSerials().isEmpty() &&
                    importerRuleLimitations.getRvmSerials().stream().noneMatch(rvmSerial::equals)) {
                return null;
            }
        }

        return importerRule;
    }

    @Override
    public void delete(String id) {
        Optional<ImporterRule> importerRuleOptional = importerRuleRepository.findById(id);
        importerRuleOptional.ifPresent(importerRuleRepository::delete);
    }

    @Override
    public List<ImporterRule> getAllByRvmOwnerAndRvmSerial(String rvmOwner, List<String> rvmSerial) {
        return importerRuleLimitationsRepository.findAllByRvmOwner(rvmOwner)
                .stream()
                .filter(limitation -> filterRvmSerials(limitation, rvmSerial))
                .map(ImporterRuleLimitations::getImporterRuleId)
                .distinct()
                .map(importerRuleRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private boolean filterRvmSerials(ImporterRuleLimitations importerRuleLimitations, List<String> rvmSerials) {
        List<String> limitedRvmSerials = importerRuleLimitations.getRvmSerials();

        return limitedRvmSerials == null || limitedRvmSerials.isEmpty() || !Collections.disjoint(limitedRvmSerials, rvmSerials);
    }

    @Override
    public boolean remove(List<ImporterRule> importerRules) {
        List<ImporterRuleLimitations> limitationsToDelete = new ArrayList<>();
        importerRules
                .forEach(rule ->
                        limitationsToDelete.addAll(
                                importerRuleLimitationsRepository.findAllByImporterRuleId(rule.getId())));

        importerRuleLimitationsRepository.deleteAll(limitationsToDelete);
        importerRuleRepository.deleteAll(importerRules);
        return true;
    }

    @Override
    public List<ImporterRule> findAllNotIn(List<ImporterRule> importerRules) {
        return importerRuleRepository.findAllByIdNotIn(importerRules.stream()
                .map(ImporterRule::getId)
                .collect(Collectors.toList()));
    }
}
