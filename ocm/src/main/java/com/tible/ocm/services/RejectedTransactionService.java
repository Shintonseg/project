package com.tible.ocm.services;

import com.tible.ocm.models.mongo.RejectedTransaction;

import java.time.LocalDateTime;
import java.util.List;

import static com.tible.ocm.models.mongo.RejectedTransaction.TransactionType;

public interface RejectedTransactionService {

    RejectedTransaction save(RejectedTransaction rejectedTransaction);

    List<RejectedTransaction> findAllByCompanyNumber(String companyNumber);

    boolean existsByCompanyNumberAndCreatedAtAndBaseFileName(String companyNumber, LocalDateTime createdAt, String baseFileName);

    RejectedTransaction findByCompanyNumberAndCreatedAtAndBaseFileName(String companyNumber, LocalDateTime createdAt, String baseFileName);

    List<RejectedTransaction> findAllByBaseFileNameAndType(String baseFileName, TransactionType type);

    void deleteAll(List<RejectedTransaction> rejectedTransactions);

    List<RejectedTransaction> findAllNeedToBeDeletedByCompanyNumber(String companyNumber);
}
