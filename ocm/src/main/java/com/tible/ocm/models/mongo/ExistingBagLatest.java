package com.tible.ocm.models.mongo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document
@NoArgsConstructor
public class ExistingBagLatest {
    @Id
    private String id;
    private Integer label;
    private String customerNumber;
    private String rvmOwnerNumber;
    @Indexed
    private String combinedCustomerNumberLabel;
    private LocalDate createdDate;

    public ExistingBagLatest(com.tible.ocm.models.mysql.ExistingBag existingBag) {
        this.label = existingBag.getLabel();
        this.customerNumber = existingBag.getCustomerNumber();
        this.rvmOwnerNumber = existingBag.getRvmOwnerNumber();
        this.combinedCustomerNumberLabel = existingBag.getCombinedCustomerNumberLabel();
        this.createdDate = existingBag.getCreatedDate();
    }

    public static ExistingBagLatest from(com.tible.ocm.models.mysql.ExistingBag existingBag) {
        return existingBag != null ? new ExistingBagLatest(existingBag) : null;
    }
}
