package com.tible.ocm.models.mongo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class Company {

    @Id
    private String id;
    private String name;
    private String number;
    private String type;
    private String version;
    private String storeId;
    private boolean usingIpTrunking;
    private String ipAddress;
    private String ipRange;
    private String communication;
    private List<String> serialNumbers;
    private String rvmOwnerNumber;
    private String localizationNumber;
    private Integer allowDataYoungerThanDays;
    private boolean isNotifyAboutDoubleTransactions;
}
