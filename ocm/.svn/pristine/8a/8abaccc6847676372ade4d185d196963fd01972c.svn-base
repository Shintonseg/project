package com.tible.ocm.models.mongo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImporterRule {

    @Id
    private String id;
    private String fromEan;
    private String toEan;
    private String articleDescription;
}
