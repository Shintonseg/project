package com.tible.ocm.models.mysql;

import com.tible.hawk.core.models.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ExportedTransaction extends BaseEntity {

    @Column
    private String transactionNumber;
    @Basic(fetch = FetchType.LAZY)
    @Lob
    @Column
    private String value;
    @Column
    private LocalDate createdDate;

}

