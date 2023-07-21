package com.tible.ocm.services;

import com.tible.ocm.models.mysql.ExistingBag;
import com.tible.ocm.models.mongo.ExistingBagLatest;

public interface ExistingBagLatestService {

    ExistingBagLatest saveExistingBag(ExistingBag existingBag);

    void deleteByPeriod(Integer deleteOlderThan);

    boolean existsByCombinedCustomerNumberLabel(String combinedCustomerNumberLabel);
}
