package com.tible.ocm.models.mongo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document
public class SrnRemovedArticle{

    @Id
    private String id;
    private String number;
    private LocalDateTime deactivationDate;
    private LocalDateTime createdDateTime;

}
