package com.tible.ocm.services;

import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.models.mongo.TransactionArticle;

import java.util.List;
import java.util.Optional;

public interface TransactionService {

    List<Transaction> findAll();

    OcmTransactionResponse handleTransaction(TransactionDto transactionDto, String remoteAddress);

    Transaction saveTransaction(TransactionDto transactionDto, Company company);

    void moveTransactionRestToQueue(TransactionDto transactionDto, Company company);

    Transaction saveTransactionAndArticlesByCompany(Transaction transaction, List<TransactionArticle> transactionArticles, Company company);

    Transaction save(Transaction transaction);

    List<Transaction> saveAll(List<Transaction> transactions);

    List<TransactionArticle> saveTransactionArticles(List<TransactionArticle> articles);

    List<Transaction> findByTransactionNumber(String transactionNumber);

    void delete(Transaction transaction);

    void deleteAll(List<Transaction> transactions);

    List<Transaction> findAllByCompanyId(String companyId);

    int countAllByTransactionId(String transactionId);

    int countAllByTransactionIdAndRefund(String transactionId, Integer refund);

    int countAllByTransactionIdAndCollected(String transactionId, Integer collected);

    List<TransactionArticle> findAllByTransactionId(String transactionId);

    Optional<Transaction> findById(String id);
}
