package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.repositories.mongo.RejectedTransactionRepository;
import com.tible.ocm.services.RejectedTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class RejectedTransactionServiceImpl implements RejectedTransactionService {

    private final RejectedTransactionRepository rejectedTransactionRepository;

    public RejectedTransactionServiceImpl(RejectedTransactionRepository rejectedTransactionRepository) {
        this.rejectedTransactionRepository = rejectedTransactionRepository;
    }

    @Override
    public RejectedTransaction save(RejectedTransaction rejectedTransaction) {
        return rejectedTransactionRepository.save(rejectedTransaction);
    }

    @Override
    public List<RejectedTransaction> findAllByCompanyNumber(String companyNumber) {
        return rejectedTransactionRepository.findAllByCompanyNumber(companyNumber);
    }

    @Override
    public boolean existsByCompanyNumberAndCreatedAtAndBaseFileName(String companyNumber,
                                                                    LocalDateTime createdAt,
                                                                    String baseFileName) {
        return rejectedTransactionRepository
                .existsByCompanyNumberAndCreatedAtEqualsAndBaseFileName(companyNumber, createdAt, baseFileName);
    }

    @Override
    public RejectedTransaction findByCompanyNumberAndCreatedAtAndBaseFileName(String companyNumber,
                                                                              LocalDateTime createdAt,
                                                                              String baseFileName) {
        return rejectedTransactionRepository
                .findByCompanyNumberAndCreatedAtEqualsAndBaseFileName(companyNumber, createdAt, baseFileName);
    }

    @Override
    public List<RejectedTransaction> findAllByBaseFileNameAndType(String baseFileName, RejectedTransaction.TransactionType type) {
        return rejectedTransactionRepository.findAllByBaseFileNameAndType(baseFileName, type);
    }

    @Override
    public void deleteAll(List<RejectedTransaction> rejectedTransactions) {
        rejectedTransactionRepository.deleteAll(rejectedTransactions);
    }

    @Override
    public List<RejectedTransaction> findAllNeedToBeDeletedByCompanyNumber(String companyNumber) {
        return rejectedTransactionRepository.findAllByCompanyNumberAndNeedToBeDeletedIsTrue(companyNumber);
    }
}
