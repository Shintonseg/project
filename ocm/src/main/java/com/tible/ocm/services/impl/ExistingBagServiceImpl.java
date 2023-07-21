package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.ExistingBagLatest;
import com.tible.ocm.models.mysql.ExistingBag;
import com.tible.ocm.repositories.mongo.ExistingBagLatestRepository;
import com.tible.ocm.repositories.mysql.ExistingBagRepository;
import com.tible.ocm.services.ExistingBagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class ExistingBagServiceImpl implements ExistingBagService {

    private final ExistingBagRepository existingBagRepository;
    private final ExistingBagLatestRepository existingBagLatestRepository;

    @Override
    public List<ExistingBag> findAll() {
        return existingBagRepository.findAll();
    }

    @Override
    public List<ExistingBag> findAllByRvmOwnerNumber(String rvmOwnerNumber) {
        return existingBagRepository.findAllByRvmOwnerNumber(rvmOwnerNumber);
    }

    @Override
    public List<ExistingBag> findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(String rvmOwnerNumber, LocalDate createdDate) {
        return existingBagRepository.findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(rvmOwnerNumber, createdDate);
    }

    @Override
    public List<ExistingBagLatest> findAllLatest() {
        return existingBagLatestRepository.findAll();
    }

    @Override
    public ExistingBag findByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel) {
        return existingBagRepository.findByCombinedCustomerNumberLabel(combinedCustomerNumberLabel);
    }

    @Override
    public boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel) {
        return existingBagRepository.existsByCombinedCustomerNumberLabel(combinedCustomerNumberLabel);
    }

    @Override
    public ExistingBag save(ExistingBag existingTransaction) {
        return existingBagRepository.save(existingTransaction);
    }

    @Override
    public void deleteAll(List<ExistingBag> existingTransactions) {
        existingBagRepository.deleteAll(existingTransactions);
    }

    @Override
    public boolean lazyCheckIsBagAlreadyExists(String label) {
        if (existingBagLatestRepository.existsByCombinedCustomerNumberLabel(label)) {
            return true;
        }
        return existsByCombinedCustomerNumberLabel(label);
    }
}
