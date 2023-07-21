package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.RejectedTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RejectedTransactionRepository extends MongoRepository<RejectedTransaction, String> {

    List<RejectedTransaction> findAllByCompanyNumber(String companyNumber);

    boolean existsByCompanyNumberAndCreatedAtEqualsAndBaseFileName(String companyNumber, LocalDateTime createdAt, String baseFileName);

    RejectedTransaction findByCompanyNumberAndCreatedAtEqualsAndBaseFileName(String companyNumber, LocalDateTime createdAt, String baseFileName);

    List<RejectedTransaction> findAllByBaseFileNameAndType(String baseFileName, RejectedTransaction.TransactionType type);

    List<RejectedTransaction> findAllByCompanyNumberAndNeedToBeDeletedIsTrue(String companyNumber);
}
