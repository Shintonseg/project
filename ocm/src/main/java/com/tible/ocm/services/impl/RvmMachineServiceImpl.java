package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.RvmMachine;
import com.tible.ocm.models.mongo.RvmSupplier;
import com.tible.ocm.repositories.mongo.RvmMachineRepository;
import com.tible.ocm.repositories.mongo.RvmSupplierRepository;
import com.tible.ocm.services.RvmMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Primary
@Service
public class RvmMachineServiceImpl implements RvmMachineService {

    private final RvmSupplierRepository rvmSupplierRepository;
    private final RvmMachineRepository rvmMachineRepository;

    public RvmMachineServiceImpl(RvmSupplierRepository rvmSupplierRepository,
                                 RvmMachineRepository rvmMachineRepository) {
        this.rvmSupplierRepository = rvmSupplierRepository;
        this.rvmMachineRepository = rvmMachineRepository;
    }

    @Override
    public List<RvmMachine> findAll() {
        return rvmMachineRepository.findAll();
    }

    @Override
    public RvmMachine save(RvmSupplier rvmSupplier, RvmMachine rvmMachine) {
        RvmMachine savedRvmMachine = rvmMachineRepository.save(rvmMachine);
        if (rvmSupplier.getRvmMachines().stream().noneMatch(oldRvmMachine -> oldRvmMachine.getId().equals(savedRvmMachine.getId()))) {
            rvmSupplier.getRvmMachines().add(savedRvmMachine);
            rvmSupplierRepository.save(rvmSupplier);
        }
        return savedRvmMachine;
    }

    @Override
    public void delete(String id) {
        rvmMachineRepository.deleteById(id);
    }
}
