package com.tible.ocm.services;

import com.tible.ocm.models.mongo.LabelOrder;

import java.util.List;

public interface LabelOrderService {

    List<LabelOrder> findAll();

    List<LabelOrder> findAllByRvmOwnerNumberAndCustomerLocalizationNumber(String rvmOwnerNumber, String customerLocalizationNumber);

    LabelOrder findByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(String customerNumber, String rvmOwnerNumber, Long firstLabelNumber);

    boolean existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(String customerNumber, String rvmOwnerNumber, Long firstLabelNumber);

    boolean existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(String customerNumber, String rvmOwnerNumber, Long labelNumber);

    boolean existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(String customerNumber, Long labelNumber);

    LabelOrder save(LabelOrder labelOrder);

    void deleteAll(List<LabelOrder> labelOrders);
}
