package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.ExistingTransactionLatest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface ExistingTransactionLatestRepository extends MongoRepository<ExistingTransactionLatest, String> {

    boolean existsByNumberAndRvmOwnerNumber(String number, String rvmOwnerNumber);

    Integer deleteByCreatedDateLessThanEqual(LocalDate deleteDate);
}
