package com.tible.ocm.dto;

import com.tible.hawk.core.configurations.Finder;
import com.tible.ocm.models.mysql.ExistingTransaction;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ExistingTransactionDto {

    private Long id;
    private String number;
    private String customerNumber;
    private String rvmOwnerNumber;
    private String transactionCombinedNumber;
    private LocalDate createdDate;
    private String combinedCustomerNumberLabel;

    public ExistingTransactionDto(ExistingTransaction existingTransaction) {
        this.id = existingTransaction.getId();
        this.number = existingTransaction.getNumber();
        this.customerNumber = existingTransaction.getCustomerNumber();
        this.rvmOwnerNumber = getRvmOwnerNumber();
        this.transactionCombinedNumber = existingTransaction.getTransactionCombinedNumber();
        this.createdDate = existingTransaction.getCreatedDate();
        this.combinedCustomerNumberLabel = existingTransaction.getCombinedCustomerNumberLabel();
    }

    public static ExistingTransactionDto from(ExistingTransaction existingTransaction) {
        return existingTransaction == null ? null : new ExistingTransactionDto(existingTransaction);
    }

    public ExistingTransaction toEntity(Finder finder) {
        ExistingTransaction existingTransaction = this.getId() == null ? new ExistingTransaction() : finder.find(ExistingTransaction.class, this.getId());

        existingTransaction.setNumber(this.number);
        existingTransaction.setCustomerNumber(this.customerNumber);
        existingTransaction.setRvmOwnerNumber(this.rvmOwnerNumber);
        existingTransaction.setTransactionCombinedNumber(this.transactionCombinedNumber);
        existingTransaction.setCreatedDate(this.createdDate);
        existingTransaction.setCombinedCustomerNumberLabel(this.combinedCustomerNumberLabel);

        return existingTransaction;
    }
}
