package com.tible.ocm.services;

import com.tible.ocm.models.mongo.RvmMachine;
import com.tible.ocm.models.mongo.RvmSupplier;

import java.util.List;

public interface RvmMachineService {

    List<RvmMachine> findAll();

    RvmMachine save(RvmSupplier rvmSupplier, RvmMachine rvmMachine);

    void delete(String id);

}
