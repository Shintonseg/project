package com.tible.ocm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tible.ocm.models.mongo.ImporterRuleLimitations;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImporterRuleLimitationsDto {

    private String id;
    private String rvmOwner;
    private List<String> rvmSerials;

    public ImporterRuleLimitationsDto(ImporterRuleLimitations importerRuleLimitations) {
        this.id = importerRuleLimitations.getId();
        this.rvmOwner = importerRuleLimitations.getRvmOwner();

        ofNullable(importerRuleLimitations.getRvmSerials())
                .map(ArrayList::new)
                .ifPresent(this::setRvmSerials);
    }

    public static ImporterRuleLimitationsDto from(ImporterRuleLimitations importerRuleLimitations) {
        return ofNullable(importerRuleLimitations)
                .map(ImporterRuleLimitationsDto::new)
                .orElse(null);
    }

    public static List<ImporterRuleLimitationsDto> fromList(List<ImporterRuleLimitations> importerRuleLimitations) {
        return importerRuleLimitations
                .stream()
                .map(ImporterRuleLimitationsDto::from)
                .collect(Collectors.toList());
    }

    public ImporterRuleLimitations toEntity(MongoTemplate mongoTemplate) {
        ImporterRuleLimitations importerRuleLimitations = ofNullable(this.id)
                .map(it -> mongoTemplate.findById(it, ImporterRuleLimitations.class))
                .orElseGet(ImporterRuleLimitations::new);

        importerRuleLimitations.setRvmOwner(this.rvmOwner);

        ofNullable(this.rvmSerials)
                .map(ArrayList::new)
                .ifPresent(importerRuleLimitations::setRvmSerials);

        return importerRuleLimitations;
    }
}
