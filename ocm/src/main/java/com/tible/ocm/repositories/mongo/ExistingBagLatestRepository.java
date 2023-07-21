package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.ExistingBagLatest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;

public interface ExistingBagLatestRepository extends MongoRepository<ExistingBagLatest, String> {

    boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);

    Integer deleteByCreatedDateLessThanEqual(LocalDate date);
}
