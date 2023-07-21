package com.tible.ocm.models.mongo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImporterRuleLimitations {

    @Id
    private String id;
    @Indexed
    private String importerRuleId;
    private String rvmOwner; // rvm owner company number
    private List<String> rvmSerials;
}
