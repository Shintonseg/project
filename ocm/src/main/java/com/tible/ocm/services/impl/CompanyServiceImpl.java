package com.tible.ocm.services.impl;

import com.tible.ocm.models.CharitiesResponse;
import com.tible.ocm.models.CharityResponse;
import com.tible.ocm.models.OcmVersion;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.RefundArticle;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.models.mongo.TransactionArticle;
import com.tible.ocm.repositories.mongo.CompanyRepository;
import com.tible.ocm.repositories.mongo.RefundArticleRepository;
import com.tible.ocm.repositories.mongo.TransactionArticleRepository;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.CompanyService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
public class CompanyServiceImpl implements CompanyService {

    public static final String CHARITY_TYPE = "CHARITY";

    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final RefundArticleRepository refundArticleRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionArticleRepository transactionArticleRepository;

    public CompanyServiceImpl(PasswordEncoder passwordEncoder,
                              CompanyRepository companyRepository,
                              RefundArticleRepository refundArticleRepository,
                              TransactionRepository transactionRepository,
                              TransactionArticleRepository transactionArticleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
        this.refundArticleRepository = refundArticleRepository;
        this.transactionRepository = transactionRepository;
        this.transactionArticleRepository = transactionArticleRepository;
    }

    @Override
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    @Override
    public Company findByNumber(String number) {
        return companyRepository.findByNumber(number);
    }

    @Override
    public Company findByStoreIdAndRvmOwnerNumber(String storeId, String rvmOwnerNumber) {
        return companyRepository.findByStoreIdAndRvmOwnerNumber(storeId, rvmOwnerNumber);
    }

    @Override
    public Company findFirstByIpAddress(String ipAddress) {
        return companyRepository.findFirstByIpAddress(ipAddress);
    }

    @Override
    public Company save(Company company) {
        return companyRepository.save(company);
    }

    @Override
    public void delete(String id) {
        Optional<Company> optionalCompany = companyRepository.findById(id);
        optionalCompany.ifPresent(company -> {
            List<RefundArticle> refundArticles = refundArticleRepository.findAllByCompanyId(company.getId());
            if (!CollectionUtils.isEmpty(refundArticles)) {
                refundArticleRepository.deleteAll(refundArticles);
            }
            List<Transaction> transactions = transactionRepository.findAllByCompanyId(company.getId());
            if (!CollectionUtils.isEmpty(transactions)) {
                transactions.forEach(transaction -> {
                    List<TransactionArticle> transactionArticles = transactionArticleRepository.findAllByTransactionId(transaction.getId());
                    transactionArticles.forEach(transactionArticleRepository::delete);
                    transactionRepository.delete(transaction);
                });
            }
            companyRepository.delete(company);
        });
    }

    @Override
    public boolean existsByNumber(String number) {
        return companyRepository.existsByNumber(number);
    }

    @Override
    public boolean existsByStoreIdAndRvmOwnerNumber(String storeId, String rvmOwnerNumber) {
        return companyRepository.existsByStoreIdAndRvmOwnerNumber(storeId, rvmOwnerNumber);
    }

    @Override
    public Optional<Company> findById(String id) {
        return companyRepository.findById(id);
    }

    @Override
    public List<Company> findByLocalizationNumberAndRvmOwnerNumber(String localizationNumber, String rvmOwnerNumber) {
        return companyRepository.findAllByLocalizationNumberAndRvmOwnerNumber(localizationNumber, rvmOwnerNumber);
    }

    @Override
    public CharitiesResponse getAllCharities(String version) {
        List<CharityResponse> charities = companyRepository.findAllByType(CHARITY_TYPE)
                .stream()
                .map(company -> CharityResponse.builder().number(company.getNumber()).name(company.getName()).build())
                .collect(Collectors.toList());

        return CharitiesResponse.builder()
                .version(OcmVersion.VERSION_17.title)
                .dateTime(LocalDateTime.now())
                .charities(charities)
                .total(charities.size())
                .build();
    }

    @Override
    public List<Company> findAllCharities() {
        return companyRepository.findAllByType(CHARITY_TYPE);
    }

    @Override
    public boolean existsByTypeAndNumber(String type, String number) {
        return companyRepository.existsByTypeAndNumber(type, number);
    }

    @Override
    public int getDataExpirationPeriodInDays(Company company) {
        Company rvmOwner = findByNumber(company.getRvmOwnerNumber());
        return rvmOwner.getAllowDataYoungerThanDays() != null ? rvmOwner.getAllowDataYoungerThanDays() : 30;
    }
}
