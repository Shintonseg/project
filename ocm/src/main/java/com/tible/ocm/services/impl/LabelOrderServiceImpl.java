package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.LabelOrder;
import com.tible.ocm.repositories.mongo.LabelOrderRepository;
import com.tible.ocm.services.LabelOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Primary
@Service
public class LabelOrderServiceImpl implements LabelOrderService {

    private final LabelOrderRepository labelOrderRepository;

    public LabelOrderServiceImpl(LabelOrderRepository labelOrderRepository) {
        this.labelOrderRepository = labelOrderRepository;
    }

    @Override
    public List<LabelOrder> findAll() {
        return labelOrderRepository.findAll();
    }

    @Override
    public List<LabelOrder> findAllByRvmOwnerNumberAndCustomerLocalizationNumber(String rvmOwnerNumber, String customerLocalizationNumber) {
        return labelOrderRepository.findAllByRvmOwnerNumberAndCustomerLocalizationNumber(rvmOwnerNumber, customerLocalizationNumber);
    }

    @Override
    public LabelOrder findByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(String customerNumber, String rvmOwnerNumber, Long firstLabelNumber) {
        return labelOrderRepository.findByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(customerNumber, rvmOwnerNumber, firstLabelNumber);
    }

    @Override
    public boolean existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(String customerNumber, String rvmOwnerNumber, Long firstLabelNumber) {
        return labelOrderRepository.existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(customerNumber, rvmOwnerNumber, firstLabelNumber);
    }

    @Override
    public boolean existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(String customerNumber, String rvmOwnerNumber, Long labelNumber) {
        return labelOrderRepository.existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumberIsLessThanEqualAndLastLabelNumberGreaterThanEqualAndMarkAllLabelsAsUsedFalse(customerNumber, rvmOwnerNumber, labelNumber, labelNumber);
    }

    @Override
    public boolean existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(String customerNumber, Long labelNumber) {
        return labelOrderRepository.existsByCustomerNumberAndFirstLabelNumberIsLessThanEqualAndLastLabelNumberGreaterThanEqualAndMarkAllLabelsAsUsedFalse(customerNumber, labelNumber, labelNumber);
    }

    @Override
    public LabelOrder save(LabelOrder labelOrder) {
        return labelOrderRepository.save(labelOrder);
    }

    @Override
    public void deleteAll(List<LabelOrder> labelOrders) {
        labelOrderRepository.deleteAll(labelOrders);
    }
}
