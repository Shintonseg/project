package com.tible.ocm.models.mongo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Document
public class ExistingTransactionLatest {
    @Id
    private String id;
    @Indexed
    private String number;
    private String customerNumber;
    private String rvmOwnerNumber;
    private String transactionCombinedNumber;
    private LocalDate createdDate;

    public ExistingTransactionLatest(com.tible.ocm.models.mysql.ExistingTransaction existingTransaction) {
        this.number = existingTransaction.getNumber();
        this.customerNumber = existingTransaction.getCustomerNumber();
        this.rvmOwnerNumber = existingTransaction.getRvmOwnerNumber();
        this.transactionCombinedNumber = existingTransaction.getTransactionCombinedNumber();
        this.createdDate = existingTransaction.getCreatedDate();
    }

    public static ExistingTransactionLatest from(com.tible.ocm.models.mysql.ExistingTransaction existingTransaction) {
        return existingTransaction != null ? new ExistingTransactionLatest(existingTransaction) : null;
    }
}
