package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    boolean existsByTransactionNumber(String transactionNumber);

    boolean existsByLabelNumber(String labelNumber);

    boolean existsByDateTimeAndStoreIdAndSerialNumber(LocalDateTime dateTime, String storeId, String serialNumber);

    List<Transaction> findByTransactionNumber(String transactionNumber);

    List<Transaction> findAllByCompanyId(String companyId);

    List<Transaction> findAllByDateTimeGreaterThanEqualAndReceivedDateLessThanEqual(LocalDateTime from, LocalDateTime to);
}
