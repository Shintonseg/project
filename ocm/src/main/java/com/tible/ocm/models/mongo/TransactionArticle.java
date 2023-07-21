package com.tible.ocm.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class TransactionArticle {

    @Id
    private String id;
    private String articleNumber;
    private Integer scannedWeight;
    private Integer material;
    private Integer refund;
    private Integer collected;
    private Integer manual;

    @Indexed
    private String transactionId;

}
