package com.tible.ocm.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.models.mysql.ExportedTransaction;
import com.tible.ocm.repositories.mysql.ExportedTransactionRepository;
import com.tible.ocm.services.ExportedTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Primary
@Service
public class ExportedTransactionServiceImpl implements ExportedTransactionService {

    private final ExportedTransactionRepository exportedTransactionRepository;
    private final ObjectMapper objectMapper;

    public ExportedTransactionServiceImpl(ExportedTransactionRepository exportedTransactionRepository,
                                          ObjectMapper objectMapper) {
        this.exportedTransactionRepository = exportedTransactionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Transaction findByTransactionNumber(Long transactionNumber) {
        ExportedTransaction exportedTransaction = exportedTransactionRepository.findByTransactionNumber(transactionNumber);
        try {
            return objectMapper.readValue(exportedTransaction.getValue(), Transaction.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void save(Transaction transaction) {
        try {
            exportedTransactionRepository.save(buildExportedTransaction(transaction));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void saveAll(List<Transaction> transactions) {
        List<ExportedTransaction> exportedTransactions = Lists.newArrayList();
        transactions.forEach(t -> {
            try {
                exportedTransactions.add(buildExportedTransaction(t));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        });
        exportedTransactionRepository.saveAll(exportedTransactions);
    }

    private ExportedTransaction buildExportedTransaction(Transaction transaction) throws JsonProcessingException {
        return new ExportedTransaction(
                transaction.getTransactionNumber(),
                objectMapper.writeValueAsString(transaction),
                LocalDate.now());
    }

    @Override
    public void deleteByPeriod(int days) {
        List<ExportedTransaction> transactionsToDelete = exportedTransactionRepository
                .findByCreatedDateLessThanEqual(LocalDate.now().minusDays(days));
        log.info("Deleting {} transactions after {} days.", transactionsToDelete.size(), days);
        exportedTransactionRepository.deleteAll(transactionsToDelete);
    }
}
