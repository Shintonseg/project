package com.tible.ocm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tible.ocm.models.mongo.Company;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyDto {

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

    public CompanyDto(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.number = company.getNumber();
        this.type = company.getType();
        this.version = company.getVersion();
        this.storeId = company.getStoreId();
        this.usingIpTrunking = company.isUsingIpTrunking();
        this.ipAddress = company.getIpAddress();
        this.ipRange = company.getIpRange();
        this.serialNumbers = company.getSerialNumbers();
        this.communication = company.getCommunication();
        this.rvmOwnerNumber = company.getRvmOwnerNumber();
        this.localizationNumber = company.getLocalizationNumber();
        this.allowDataYoungerThanDays = company.getAllowDataYoungerThanDays();
        this.isNotifyAboutDoubleTransactions = company.isNotifyAboutDoubleTransactions();
    }

    public static CompanyDto from(Company company) {
        return company == null ? null : new CompanyDto(company);
    }

    public Company toEntity(MongoTemplate mongoTemplate) {
        Company company = this.id != null ? mongoTemplate.findById(this.id, Company.class) : new Company();
        company = company != null ? company : new Company();

        company.setName(this.name);
        company.setNumber(this.number);
        company.setType(this.type);
        company.setVersion(this.version);
        company.setStoreId(this.storeId);
        company.setUsingIpTrunking(this.usingIpTrunking);
        company.setIpAddress(this.ipAddress);
        company.setIpRange(this.ipRange);
        company.setSerialNumbers(this.serialNumbers);
        company.setCommunication(this.communication);
        company.setRvmOwnerNumber(this.rvmOwnerNumber);
        company.setLocalizationNumber(this.localizationNumber);
        company.setAllowDataYoungerThanDays(this.allowDataYoungerThanDays);
        company.setNotifyAboutDoubleTransactions(this.isNotifyAboutDoubleTransactions);

        return company;
    }
}
