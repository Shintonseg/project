package com.tible.ocm.services.impl;

import com.tible.ocm.models.mysql.ExistingBag;
import com.tible.ocm.models.mongo.ExistingBagLatest;
import com.tible.ocm.repositories.mongo.ExistingBagLatestRepository;
import com.tible.ocm.services.ExistingBagLatestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExistingBagLatestServiceImpl implements ExistingBagLatestService {

    private final ExistingBagLatestRepository existingBagLatestRepository;

    @Override
    public ExistingBagLatest saveExistingBag(ExistingBag existingBag) {
        return existingBagLatestRepository.save(ExistingBagLatest.from(existingBag));
    }

    @Override
    public void deleteByPeriod(Integer deleteOlderThan) {
        LocalDate deleteDate = LocalDate.now().minusDays(deleteOlderThan);
        Integer countDeletedBags = existingBagLatestRepository.deleteByCreatedDateLessThanEqual(deleteDate);
        log.info("Delete {} bags older then {}", countDeletedBags, deleteDate);
    }

    @Override
    public boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel) {
        return existingBagLatestRepository.existsByCombinedCustomerNumberLabel(combinedCustomerNumberLabel);
    }
}
