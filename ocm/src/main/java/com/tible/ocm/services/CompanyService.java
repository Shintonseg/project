package com.tible.ocm.services;

import com.tible.ocm.models.CharitiesResponse;
import com.tible.ocm.models.mongo.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyService {

    List<Company> findAll();

    Optional<Company> findById(String id);

    Company findByNumber(String number);

    Company findByStoreIdAndRvmOwnerNumber(String storeId, String rvmOwnerNumber);

    Company findFirstByIpAddress(String ipAddress);

    Company save(Company company);

    boolean existsByNumber(String number);

    boolean existsByStoreIdAndRvmOwnerNumber(String storeId, String rvmOwnerNumber);

    void delete(String id);

    List<Company> findByLocalizationNumberAndRvmOwnerNumber(String localizationNumber, String rvmOwnerNumber);

    CharitiesResponse getAllCharities(String version);

    List<Company> findAllCharities();

    boolean existsByTypeAndNumber(String type, String number);

    int getDataExpirationPeriodInDays(Company company);
}
