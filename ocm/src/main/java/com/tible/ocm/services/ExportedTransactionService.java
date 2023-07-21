package com.tible.ocm.services;

import com.tible.ocm.models.mongo.Transaction;

import java.util.List;

public interface ExportedTransactionService {

    Transaction findByTransactionNumber(Long transactionNumber);

    void save(Transaction transaction);

    void saveAll(List<Transaction> transactions);

    void deleteByPeriod(int days);

}
