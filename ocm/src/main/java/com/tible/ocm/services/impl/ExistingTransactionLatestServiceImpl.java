package com.tible.ocm.services.impl;

import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.models.mongo.ExistingTransactionLatest;
import com.tible.ocm.repositories.mongo.ExistingTransactionLatestRepository;
import com.tible.ocm.services.ExistingTransactionLatestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExistingTransactionLatestServiceImpl implements ExistingTransactionLatestService {
    private final ExistingTransactionLatestRepository existingTransactionLatestRepository;

    @Override
    public ExistingTransactionLatest saveExistingTransaction(ExistingTransaction existingTransaction) {
        return existingTransactionLatestRepository.save(ExistingTransactionLatest.from(existingTransaction));
    }

    @Override
    public void deleteByPeriod(Integer deleteOlderThan) {
        LocalDate deleteDate = LocalDate.now().minusDays(deleteOlderThan);
        Integer countDeletedTransactions = existingTransactionLatestRepository.deleteByCreatedDateLessThanEqual(deleteDate);
        log.info("Delete {} transactions older then {}", countDeletedTransactions, deleteDate);
    }

    @Override
    public boolean existsByTransactionNumberAndRvmOwnerNumber(String number, String rvmOwnerNumber) {
        return existingTransactionLatestRepository.existsByNumberAndRvmOwnerNumber(number, rvmOwnerNumber);
    }
}
