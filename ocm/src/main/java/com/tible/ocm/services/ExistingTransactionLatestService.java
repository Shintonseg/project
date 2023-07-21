package com.tible.ocm.services;

import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.models.mongo.ExistingTransactionLatest;

public interface ExistingTransactionLatestService {

    ExistingTransactionLatest saveExistingTransaction(ExistingTransaction existingTransaction);

    void deleteByPeriod(Integer deleteOlderThan);

    boolean existsByTransactionNumberAndRvmOwnerNumber(String number, String rvmOwnerNumber);
}
