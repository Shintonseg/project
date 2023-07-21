package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.RefundArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundArticleRepository extends MongoRepository<RefundArticle, String> {

    boolean existsByNumber(String number);

    Optional<RefundArticle> findByNumber(String number);

    List<RefundArticle> findAllByCompanyId(String companyId);
}
