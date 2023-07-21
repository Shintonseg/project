package com.tible.ocm.models.mysql;

import com.tible.hawk.core.models.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = {@Index(name = "idx_ExistingBag_label", columnList = "label"),
        @Index(name = "idx_ExistingBag_rvmOwnerNumber", columnList = "rvmOwnerNumber"),
        @Index(name = "idx_ExistingBag_combinedCustomerNumberLabel", columnList = "combinedCustomerNumberLabel")})
public class ExistingBag extends BaseEntity {
    private Integer label;
    private String customerNumber;
    private String rvmOwnerNumber;
    private String combinedCustomerNumberLabel;
    private LocalDate createdDate;

}
