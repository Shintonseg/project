package com.tible.ocm.models.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
public class Transaction {

    @Id
    private String id;
    private String version;
    private LocalDateTime dateTime;
    private String storeId;
    private String serialNumber;
    private String transactionNumber;
    private Integer total;
    private Integer refundable;
    private Integer collected;
    private Integer manual;
    private Integer rejected;
    private LocalDateTime receivedDate = LocalDateTime.now();
    private String labelNumber;
    private String bagType;
    private String charityNumber;

    private String type;
    private Boolean inQueue = false;
    private LocalDateTime inQueueDateTime;
    private Boolean failed = false;

    @Indexed
    private String companyId;

}
