package com.tible.ocm.models.mongo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document
public class RefundArticle {

    @Id
    private String id;
    private String number;
    private String supplier;
    private LocalDateTime activationDate;
    private Integer weightMin;
    private Integer weightMax;
    private Integer volume;
    private Integer height;
    private Integer diameter;
    private Integer material;
    private Integer type;
    private String description;
    private Integer wildcard;

    private String companyId;

}
