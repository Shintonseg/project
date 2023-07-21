package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.Company;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends MongoRepository<Company, String> {

    Company findByNumber(String number);

    Company findByStoreIdAndRvmOwnerNumber(String storeId, String rvmOwnerNumber);

    Company findFirstByIpAddress(String ipAddress);

    boolean existsByNumber(String number);

    boolean existsByStoreIdAndRvmOwnerNumber(String storeId, String rvmOwnerNumber);

    List<Company> findAllByLocalizationNumberAndRvmOwnerNumber(String localizationNumber, String rvmOwnerNumber);

    List<Company> findAllByType(String type);

    boolean existsByTypeAndNumber(String type, String number);
}
