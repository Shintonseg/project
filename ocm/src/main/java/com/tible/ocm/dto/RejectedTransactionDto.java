package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.RejectedTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

import static java.util.Optional.ofNullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedTransactionDto {

    private String baseFileName;
    private LocalDateTime createdAt;
    private RejectedTransaction.TransactionType type;
    private String companyNumber;
    private boolean needToBeDeleted;
    private LocalDateTime deletedSince;

    public RejectedTransactionDto(RejectedTransaction rejectedTransaction) {
        this.baseFileName = rejectedTransaction.getBaseFileName();
        this.createdAt = rejectedTransaction.getCreatedAt();
        this.type = rejectedTransaction.getType();
        this.companyNumber = rejectedTransaction.getCompanyNumber();
        this.needToBeDeleted = rejectedTransaction.isNeedToBeDeleted();
        this.deletedSince = rejectedTransaction.getDeletedSince();
    }

    public static RejectedTransactionDto from(RejectedTransaction rejectedTransaction) {
        return ofNullable(rejectedTransaction)
                .map(RejectedTransactionDto::new)
                .orElse(null);
    }

    public RejectedTransaction toEntity(MongoTemplate mongoTemplate) {
        return RejectedTransaction
                .builder()
                .baseFileName(this.baseFileName)
                .createdAt(this.createdAt)
                .type(this.type)
                .companyNumber(this.companyNumber)
                .needToBeDeleted(this.needToBeDeleted)
                .deletedSince(this.deletedSince)
                .build();
    }
}
