package com.tible.ocm.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document
public class LabelOrder {

    @Id
    private String id;
    @Indexed
    private String customerNumber;
    @Indexed
    private String customerLocalizationNumber;
    @Indexed
    private String rvmOwnerNumber;
    private Long quantity;
    private Long balance;
    @Indexed
    private Long firstLabelNumber;
    private Long lastLabelNumber;
    private LocalDateTime orderDate;
    private Boolean markAllLabelsAsUsed;
}
