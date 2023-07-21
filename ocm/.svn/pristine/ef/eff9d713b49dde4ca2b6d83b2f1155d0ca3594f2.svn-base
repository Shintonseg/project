package com.tible.ocm.services;

import com.tible.ocm.models.mysql.ExistingTransaction;

import java.time.LocalDate;
import java.util.List;

public interface ExistingTransactionService {

    List<ExistingTransaction> findAll();

    List<ExistingTransaction> findAllByRvmOwnerNumber(String rvmOwnerNumber);

    List<ExistingTransaction> findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(String rvmOwnerNumber, LocalDate createdDate);

    ExistingTransaction findByTransactionNumberAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber);

    boolean existsByTransactionNumberAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber);

    boolean existsByCombinedCustomerNumberLabelAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber);

    boolean existsByCombinedCustomerNumberLabel(String transactionNumber);

    ExistingTransaction save(ExistingTransaction existingTransaction);

    void deleteAll(List<ExistingTransaction> existingTransactions);

    boolean lazyCheckIsTransactionAlreadyExists(String transactionNumber, String rvmOwnerNumber);
}
