package com.tible.ocm.repositories.mysql;

import com.tible.hawk.core.repositories.BaseCrudRepository;
import com.tible.ocm.models.mysql.ExistingBag;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExistingBagRepository extends BaseCrudRepository<ExistingBag> {

    List<ExistingBag> findAllByRvmOwnerNumber(String rvmOwnerNumber);

    List<ExistingBag> findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(String rvmOwnerNumber, LocalDate createdDate);

    ExistingBag findByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);

    boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);
}
