package com.tible.ocm.services.impl;

import com.tible.ocm.dto.RefundArticleDto;
import com.tible.ocm.dto.RefundArticles;
import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.RefundArticle;
import com.tible.ocm.repositories.mongo.RefundArticleRepository;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.RefundArticleService;
import com.tible.ocm.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.tible.ocm.models.OcmStatus.ACCEPTED;
import static com.tible.ocm.utils.ValidationUtils.decline;
import static com.tible.ocm.utils.ValidationUtils.isDateValid;

@Slf4j
@Primary
@Service
public class RefundArticleServiceImpl implements RefundArticleService {

    @Value("${ocm-version}")
    private String ocmVersion;

    private final RefundArticleRepository refundArticleRepository;
    private final CompanyService companyService;

    public RefundArticleServiceImpl(RefundArticleRepository refundArticleRepository,
                                    CompanyService companyService) {
        this.refundArticleRepository = refundArticleRepository;
        this.companyService = companyService;
    }

    @Override
    public RefundArticles findAll() {
        List<RefundArticle> articles = refundArticleRepository.findAll();
        RefundArticles refundArticles = new RefundArticles();
        refundArticles.setArticles(articles.stream().map(RefundArticleDto::from).collect(Collectors.toList()));
        refundArticles.setVersion(ocmVersion);
        refundArticles.setDateTime(LocalDateTime.now());
        refundArticles.setTotal(articles.size());
        refundArticles.setWildcard(articles.stream().mapToInt(RefundArticle::getWildcard).sum());
        return refundArticles;
    }

    @Override
    public OcmResponse saveRefundArticles(RefundArticles refundArticles, String ipAddress) {
        ipAddress = ipAddress.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ipAddress;
        Company company = companyService.findFirstByIpAddress(ipAddress);
        if (company == null) { //TODO: always point to one of the Lamson machines if the machine is not found for the request its ip address. This really needs to be removed after first POC is done.
            company = companyService.findFirstByIpAddress("10.150.0.5"); //TODO: used the 10.150.0.5 for now
        }

        OcmResponse response = validate(refundArticles, company, ipAddress);
        if (response.getStatus().equals(ACCEPTED) && company != null) {
            Company finalCompany = company;
            refundArticles.getArticles().forEach(refundArticleDto -> {
                List<RefundArticle> companyRefundArticles = findAllByCompanyId(finalCompany.getId());
                boolean refundArticleExists = companyRefundArticles.stream().anyMatch(refundArticle -> refundArticle.getNumber().equals(refundArticleDto.getNumber()));
                if (!refundArticleExists) {
                    RefundArticle convertedRefundArticle = new RefundArticle();
                    convertedRefundArticle.setNumber(refundArticleDto.getNumber());
                    convertedRefundArticle.setSupplier(refundArticleDto.getSupplier());
                    convertedRefundArticle.setActivationDate(refundArticleDto.getActivationDate());
                    convertedRefundArticle.setWeightMin(refundArticleDto.getWeightMin());
                    convertedRefundArticle.setWeightMax(refundArticleDto.getWeightMax());
                    convertedRefundArticle.setVolume(refundArticleDto.getVolume());
                    convertedRefundArticle.setHeight(refundArticleDto.getHeight());
                    convertedRefundArticle.setDiameter(refundArticleDto.getDiameter());
                    convertedRefundArticle.setMaterial(refundArticleDto.getMaterial());
                    convertedRefundArticle.setType(refundArticleDto.getType());
                    convertedRefundArticle.setDescription(refundArticleDto.getDescription());
                    convertedRefundArticle.setWildcard(refundArticleDto.getWildcard());
                    convertedRefundArticle.setCompanyId(finalCompany.getId());
                    RefundArticle savedRefundArticle = refundArticleRepository.save(convertedRefundArticle);
                } else {
                    response.addMessage(new OcmMessage(String.format("Article with number %s already exists.", refundArticleDto.getNumber())));
                    log.warn("Refund article with number {} already exists.", refundArticleDto.getNumber());
                }
            });

            companyService.save(finalCompany);
        }
        return response;
    }

    private OcmResponse validate(RefundArticles refundArticles, Company company, String ipAddress) {
        OcmMessage ocmMessage = ValidationUtils.defaultValidation(ipAddress, refundArticles.getVersion(), company);

        if (ocmMessage != null) {
            return decline(ocmMessage);
        }
        int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);

        refundArticles.getArticles().forEach(a -> {
            if (!isDateValid(a.getActivationDate(), dataExpirationPeriodInDays)) {
                log.warn("Activation date is not valid for refund article with number {}", a.getNumber());
            }
        });

        return OcmResponse.builder().status(ACCEPTED).build();
    }

    public List<RefundArticle> findAllByCompanyId(String companyId) {
        return refundArticleRepository.findAllByCompanyId(companyId);
    }
}
