package com.tible.ocm.repositories.mysql;

import com.tible.hawk.core.repositories.BaseCrudRepository;
import com.tible.ocm.models.mysql.ExistingTransaction;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExistingTransactionRepository extends BaseCrudRepository<ExistingTransaction> {

    ExistingTransaction findByNumberAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber);

    boolean existsByNumberAndRvmOwnerNumber(String transactionNumber, String rvmOwnerNumber);

    boolean existsByCombinedCustomerNumberLabelAndRvmOwnerNumber(String combinedCustomerNumberLabel, String rvmOwnerNumber);

    boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);

    List<ExistingTransaction> findAllByRvmOwnerNumber(String rvmOwnerNumber);

    List<ExistingTransaction> findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(String rvmOwnerNumber, LocalDate createdDate);
}
