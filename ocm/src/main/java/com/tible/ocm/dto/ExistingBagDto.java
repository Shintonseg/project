package com.tible.ocm.dto;

import com.tible.hawk.core.configurations.Finder;
import com.tible.ocm.models.mysql.ExistingBag;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ExistingBagDto {

    private Long id;
    private Integer label;
    private String customerNumber;
    private String rvmOwnerNumber;
    private String combinedCustomerNumberLabel;
    private LocalDate createdDate;

    public ExistingBagDto(ExistingBag existingBag) {
        this.id = existingBag.getId();
        this.label = existingBag.getLabel();
        this.customerNumber = existingBag.getCustomerNumber();
        this.rvmOwnerNumber = getRvmOwnerNumber();
        this.combinedCustomerNumberLabel = existingBag.getCombinedCustomerNumberLabel();
        this.createdDate = existingBag.getCreatedDate();
    }

    public static ExistingBagDto from(ExistingBag existingBag) {
        return existingBag == null ? null : new ExistingBagDto(existingBag);
    }

    public ExistingBag toEntity(Finder finder) {
        ExistingBag existingBag = this.getId() == null ? new ExistingBag() : finder.find(ExistingBag.class, this.getId());

        existingBag.setLabel(this.label);
        existingBag.setCustomerNumber(this.customerNumber);
        existingBag.setRvmOwnerNumber(this.rvmOwnerNumber);
        existingBag.setCombinedCustomerNumberLabel(this.combinedCustomerNumberLabel);
        existingBag.setCreatedDate(this.getCreatedDate());

        return existingBag;
    }
}
