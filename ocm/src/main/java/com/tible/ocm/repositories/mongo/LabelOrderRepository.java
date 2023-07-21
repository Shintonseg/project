package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.LabelOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LabelOrderRepository extends MongoRepository<LabelOrder, String> {

    boolean existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(String customerNumber, String rvmOwnerNumber, Long firstLabelNumber);

    boolean existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumberIsLessThanEqualAndLastLabelNumberGreaterThanEqualAndMarkAllLabelsAsUsedFalse(String customerNumber, String rvmOwnerNumber, Long firstLabelNumberCheck, Long lastLabelNumberCheck);

    boolean existsByCustomerNumberAndFirstLabelNumberIsLessThanEqualAndLastLabelNumberGreaterThanEqualAndMarkAllLabelsAsUsedFalse(String customerNumber, Long firstLabelNumberCheck, Long lastLabelNumberCheck);

    LabelOrder findByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(String customerNumber, String rvmOwnerNumber, Long firstLabelNumber);

    List<LabelOrder> findAllByRvmOwnerNumberAndCustomerLocalizationNumber(String rvmOwnerNumber, String customerLocalizationNumber);
}
