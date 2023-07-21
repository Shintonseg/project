package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.RvmSupplier;
import com.tible.ocm.models.mongo.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RvmSupplierRepository extends MongoRepository<RvmSupplier, String> {

    RvmSupplier findByRvmMachines(String rvmMachineId);

    RvmSupplier findByNumber(String number);

    RvmSupplier findByTransactionsContains(Transaction transaction);
}
