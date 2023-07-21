package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.RvmMachine;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RvmMachineRepository extends MongoRepository<RvmMachine, String> {

    RvmMachine findFirstByIpAddress(String ipAddress);

    RvmMachine findByStoreIdAndAndSerialNumber(String storeId, String serialNumber);

    boolean existsByStoreId(String storeId);

    boolean existsBySerialNumber(String serialNumber);

}
