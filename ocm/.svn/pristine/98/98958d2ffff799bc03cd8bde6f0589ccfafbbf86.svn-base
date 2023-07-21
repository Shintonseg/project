package com.tible.ocm.services;

import com.tible.ocm.models.mongo.ExistingBagLatest;
import com.tible.ocm.models.mysql.ExistingBag;

import java.time.LocalDate;
import java.util.List;

public interface ExistingBagService {

    List<ExistingBag> findAll();

    List<ExistingBag> findAllByRvmOwnerNumber(String rvmOwnerNumber);

    List<ExistingBag> findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(String rvmOwnerNumber, LocalDate createdDate);

    List<ExistingBagLatest> findAllLatest();

    ExistingBag findByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);

    boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);

    ExistingBag save(ExistingBag existingBag);

    void deleteAll(List<ExistingBag> existingBags);

    boolean lazyCheckIsBagAlreadyExists(String label);
}
