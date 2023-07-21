package com.tible.ocm.services.impl;

import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.repositories.mongo.ExistingTransactionLatestRepository;
import com.tible.ocm.repositories.mysql.ExistingTransactionRepository;
import com.tible.ocm.services.ExistingTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class ExistingTransactionServiceImpl implements ExistingTransactionService {

    private final ExistingTransactionRepository existingTransactionRepository;
    private final ExistingTransactionLatestRepository existingTransactionLatestRepository;

    @Override
    public List<ExistingTransaction> findAll() {
        return existingTransactionRepository.findAll();
    }

    @Override
    public List<ExistingTransaction> findAllByRvmOwnerNumber(String rvmOwnerNumber) {
        return existingTransactionRepository.findAllByRvmOwnerNumber(rvmOwnerNumber);
    }

    @Override
    public List<ExistingTransaction> findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(String rvmOwnerNumber, LocalDate createdDate) {
        return existingTransactionRepository.findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(rvmOwnerNumber, createdDate);
    }

    @Override
    public ExistingTransaction findByTransactionNumberAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber) {
        return existingTransactionRepository.findByNumberAndRvmOwnerNumber(transactionNumber, rvmOwnerNumber);
    }

    @Override
    public boolean existsByTransactionNumberAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber) {
        return existingTransactionRepository.existsByNumberAndRvmOwnerNumber(transactionNumber, rvmOwnerNumber);
    }

    @Override
    public boolean existsByCombinedCustomerNumberLabelAndRvmOwnerNumber(String combinedCustomerNumberLabel, String rvmOwnerNumber) {
        return existingTransactionRepository.existsByCombinedCustomerNumberLabelAndRvmOwnerNumber(combinedCustomerNumberLabel, rvmOwnerNumber);
    }

    @Override
    public boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel) {
        return existingTransactionRepository.existsByCombinedCustomerNumberLabel(combinedCustomerNumberLabel);
    }

    @Override
    public ExistingTransaction save(ExistingTransaction existingTransaction) {
        return existingTransactionRepository.save(existingTransaction);
    }

    @Override
    public void deleteAll(List<ExistingTransaction> existingTransactions) {
        existingTransactionRepository.deleteAll(existingTransactions);
    }

    @Override
    public boolean lazyCheckIsTransactionAlreadyExists(String transactionNumber, String rvmOwnerNumber) {
        if (existingTransactionLatestRepository.existsByNumberAndRvmOwnerNumber(transactionNumber, rvmOwnerNumber)) {
            return true;
        }
        return existsByTransactionNumberAndRvmOwnerNumber(transactionNumber, rvmOwnerNumber);
    }
}
