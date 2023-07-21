package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.RvmSupplier;
import com.tible.ocm.repositories.mongo.RefundArticleRepository;
import com.tible.ocm.repositories.mongo.RvmMachineRepository;
import com.tible.ocm.repositories.mongo.RvmSupplierRepository;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.RvmSupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@Service
public class RvmSupplierServiceImpl implements RvmSupplierService {

    private final PasswordEncoder passwordEncoder;
    private final RvmSupplierRepository rvmSupplierRepository;
    private final RefundArticleRepository refundArticleRepository;
    private final TransactionRepository transactionRepository;
    private final RvmMachineRepository rvmMachineRepository;

    public RvmSupplierServiceImpl(PasswordEncoder passwordEncoder,
                                  RvmSupplierRepository rvmSupplierRepository,
                                  RefundArticleRepository refundArticleRepository,
                                  TransactionRepository transactionRepository,
                                  RvmMachineRepository rvmMachineRepository) {
        this.passwordEncoder = passwordEncoder;
        this.rvmSupplierRepository = rvmSupplierRepository;
        this.refundArticleRepository = refundArticleRepository;
        this.transactionRepository = transactionRepository;
        this.rvmMachineRepository = rvmMachineRepository;
    }

    @Override
    public List<RvmSupplier> findAll() {
        return rvmSupplierRepository.findAll();
    }

    @Override
    public RvmSupplier findByNumber(String number) {
        return rvmSupplierRepository.findByNumber(number);
    }

    @Override
    public RvmSupplier save(RvmSupplier rvmSupplier) {
        rvmSupplier.setFtpPassword(passwordEncoder.encode(rvmSupplier.getFtpPassword()));

        if (!CollectionUtils.isEmpty(rvmSupplier.getRefundArticles())) {
            refundArticleRepository.saveAll(rvmSupplier.getRefundArticles());
        }
        if (!CollectionUtils.isEmpty(rvmSupplier.getTransactions())) {
            transactionRepository.saveAll(rvmSupplier.getTransactions());
        }
        if (!CollectionUtils.isEmpty(rvmSupplier.getRvmMachines())) {
            rvmMachineRepository.saveAll(rvmSupplier.getRvmMachines());
        }

        return rvmSupplierRepository.save(rvmSupplier);
    }

    @Override
    public void delete(String id) {
        Optional<RvmSupplier> optionalRvmSupplier = rvmSupplierRepository.findById(id);
        optionalRvmSupplier.ifPresent(rvmSupplier -> {
            if (!CollectionUtils.isEmpty(rvmSupplier.getRvmMachines())) {
                rvmMachineRepository.deleteAll(rvmSupplier.getRvmMachines());
            }
            if (!CollectionUtils.isEmpty(rvmSupplier.getRefundArticles())) {
                refundArticleRepository.deleteAll(rvmSupplier.getRefundArticles());
            }
            if (!CollectionUtils.isEmpty(rvmSupplier.getTransactions())) {
                transactionRepository.deleteAll(rvmSupplier.getTransactions());
            }
            rvmSupplierRepository.delete(rvmSupplier);
        });
    }
}
