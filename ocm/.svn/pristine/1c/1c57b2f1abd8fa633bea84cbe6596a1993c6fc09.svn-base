package com.tible.ocm.repositories.mysql;

import com.tible.hawk.core.repositories.BaseCrudRepository;
import com.tible.ocm.models.mysql.ExportedTransaction;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExportedTransactionRepository extends BaseCrudRepository<ExportedTransaction> {

    ExportedTransaction findByTransactionNumber(Long transactionNumber);

    List<ExportedTransaction> findByCreatedDateLessThanEqual(LocalDate createdDate);

}
