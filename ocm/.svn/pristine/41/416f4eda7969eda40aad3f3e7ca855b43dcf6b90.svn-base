package com.tible.ocm.models.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class RejectedTransaction {

    private String id;
    private String baseFileName;
    private LocalDateTime createdAt;
    private TransactionType type;
    private String companyNumber;
    private boolean isExternal;
    private boolean needToBeDeleted;
    private LocalDateTime deletedSince;

    public enum TransactionType {
        BAG,
        TRANSACTION
    }
}