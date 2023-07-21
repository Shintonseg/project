package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.TransactionArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionArticleRepository extends MongoRepository<TransactionArticle, String> {

    List<TransactionArticle> findAllByTransactionId(String transactionId);

    int countAllByTransactionId(String transactionId);

    int countAllByTransactionIdAndRefund(String transactionId, Integer refund);

    int countAllByTransactionIdAndCollected(String transactionId, Integer collected);

}
