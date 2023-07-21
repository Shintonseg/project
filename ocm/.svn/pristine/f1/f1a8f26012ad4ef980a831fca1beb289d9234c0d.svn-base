package com.tible.ocm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tible.ocm.models.mongo.ImporterRule;
import com.tible.ocm.models.mongo.ImporterRuleLimitations;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static java.util.Optional.ofNullable;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImporterRuleDto {

    private String id;
    private String fromEan;
    private String toEan;
    private String articleDescription;
    private List<ImporterRuleLimitationsDto> importerRuleLimitations;

    public ImporterRuleDto(ImporterRule importerRule, List<ImporterRuleLimitations> importerRuleLimitations) {
        this.id = importerRule.getId();
        this.fromEan = importerRule.getFromEan();
        this.toEan = importerRule.getToEan();
        this.articleDescription = importerRule.getArticleDescription();
        this.importerRuleLimitations = ImporterRuleLimitationsDto.fromList(importerRuleLimitations);
    }

    public ImporterRuleDto(ImporterRule importerRule) {
        this.id = importerRule.getId();
        this.fromEan = importerRule.getFromEan();
        this.toEan = importerRule.getToEan();
        this.articleDescription = importerRule.getArticleDescription();
    }

    public static ImporterRuleDto from(ImporterRule importerRule) {
        return ofNullable(importerRule)
                .map(ImporterRuleDto::new)
                .orElse(null);
    }

    public static ImporterRuleDto from(ImporterRule importerRule, List<ImporterRuleLimitations> importerRuleLimitations) {
        return ofNullable(importerRule)
                .map(rule -> new ImporterRuleDto(importerRule, importerRuleLimitations))
                .orElse(null);
    }

    public ImporterRule toEntity(MongoTemplate mongoTemplate) {
        ImporterRule importerRule = ofNullable(this.id)
                .flatMap(it -> ofNullable(mongoTemplate.findById(this.id, ImporterRule.class)))
                .orElseGet(ImporterRule::new);

        importerRule.setFromEan(this.fromEan);
        importerRule.setToEan(this.toEan);
        importerRule.setArticleDescription(this.articleDescription);

        return importerRule;
    }
}
