package com.tible.ocm.services.impl;

import com.tible.ocm.dto.SrnArticleDto;
import com.tible.ocm.dto.SrnArticleDtoVersion016;
import com.tible.ocm.dto.SrnArticleDtoVersion017;
import com.tible.ocm.dto.SrnArticles;
import com.tible.ocm.models.MaterialTypeCode;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.SrnArticle;
import com.tible.ocm.repositories.mongo.SrnArticleRepository;
import com.tible.ocm.services.SrnArticleService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tible.ocm.models.OcmStatus.ACCEPTED;

@Slf4j
@Primary
@Service
public class SrnArticleServiceImpl implements SrnArticleService {

    @Value("${ocm-version}")
    private String ocmVersion;

    private final SrnArticleRepository srnArticleRepository;

    public SrnArticleServiceImpl(SrnArticleRepository srnArticleRepository) {
        this.srnArticleRepository = srnArticleRepository;
    }

    @Override
    public SrnArticles<SrnArticleDto> findAll(String version) {
        List<SrnArticle> articles = getArticlesForVersion(version);
        SrnArticles<SrnArticleDto> srnArticles = new SrnArticles<>();
        srnArticles.setArticles(articles.stream().map(SrnArticleDto::from).collect(Collectors.toList()));
        srnArticles.setVersion(version);
        srnArticles.setDateTime(LocalDateTime.now());
        srnArticles.setTotal(articles.size());
        return srnArticles;
    }

    @Override
    public SrnArticles<SrnArticleDtoVersion016> findAllVersion016(String version) {
        List<SrnArticle> articles = getArticlesForVersion(version);
        SrnArticles<SrnArticleDtoVersion016> srnArticles = new SrnArticles<>();
        srnArticles.setArticles(articles.stream().map(SrnArticleDtoVersion016::from).collect(Collectors.toList()));
        srnArticles.setVersion(version);
        srnArticles.setDateTime(LocalDateTime.now());
        srnArticles.setTotal(articles.size());
        return srnArticles;
    }

    @Override
    public SrnArticles<SrnArticleDtoVersion017> findAllVersion017(String version) {
        List<SrnArticle> articles = getArticlesForVersion(version);
        SrnArticles<SrnArticleDtoVersion017> srnArticles = new SrnArticles<>();
        srnArticles.setArticles(articles.stream().map(SrnArticleDtoVersion017::from).collect(Collectors.toList()));
        srnArticles.setVersion(version);
        srnArticles.setDateTime(LocalDateTime.now());
        srnArticles.setTotal(articles.size());
        return srnArticles;
    }

    private List<SrnArticle> getArticlesForVersion(String version) {
        List<Integer> materials = new ArrayList<>();
        materials.add(MaterialTypeCode.PET.getCodeInt());
        if (ImportedFileValidationHelper.version17Check(version)) {
            materials.add(MaterialTypeCode.STEEL.getCodeInt());
            materials.add(MaterialTypeCode.ALUMINIUM.getCodeInt());
        }

        return getAllMaterialIn(materials);
    }

    @Override
    public List<SrnArticle> findAllArticles() {
        return srnArticleRepository.findAll();
    }

    @Override
    public OcmResponse saveSrnArticles(List<SrnArticle> srnArticles) {
        srnArticles.forEach(article -> {
            SrnArticle existingArticle = srnArticleRepository.findByNumber(article.getNumber());
            if (existingArticle == null) {
                article.setCreatedDateTime(LocalDateTime.now());
                srnArticleRepository.save(article);
            } else {
                srnArticleRepository.save(fillArticle(existingArticle, article));
            }
        });
        // log.info("Saved or updated {} SRN articles: {}", srnArticles.size(),
        //        srnArticles.stream().map(SrnArticle::getNumber).collect(Collectors.joining(",")));
        return OcmResponse.builder().status(ACCEPTED).build();
    }

    private SrnArticle fillArticle(SrnArticle existingArticle, SrnArticle article) {
        existingArticle.setNumber(article.getNumber());
        existingArticle.setSupplier(article.getSupplier());
        existingArticle.setWeight(article.getWeight());
        existingArticle.setVolume(article.getVolume());
        existingArticle.setHeight(article.getHeight());
        existingArticle.setDiameter(article.getDiameter());
        existingArticle.setMaterial(article.getMaterial());
        existingArticle.setDepositValue(article.getDepositValue());
        existingArticle.setShapeIdentifier(article.getShapeIdentifier());
        existingArticle.setDescription(article.getDescription());
        existingArticle.setDepositCode(article.getDepositCode());
        existingArticle.setEditedDateTime(LocalDateTime.now());
        existingArticle.setActivationDate(article.getActivationDate());
        existingArticle.setFirstArticleActivationDate(article.getFirstArticleActivationDate());
        existingArticle.setColor(article.getColor());
        return existingArticle;
    }

    @Override
    public void deleteSrnArticle(String id) {
        srnArticleRepository.deleteById(id);
    }

    @Override
    public void deleteAll(List<SrnArticle> srnArticles) {
        srnArticleRepository.deleteAll(srnArticles);
    }

    @Override
    public boolean existsAndIsActiveByArticleNumberAndRefundable(String number, Integer refund) {
        SrnArticle article = srnArticleRepository.findByNumber(number);
        return article != null && article.getActivationDate() != null &&
                article.getActivationDate().isAfter(LocalDateTime.now()) &&
                refund.equals(1);
    }

    @Override
    public boolean existsByArticleNumber(String number) {
        return srnArticleRepository.existsByNumber(number);
    }

    @Override
    public SrnArticle findByArticleNumber(String number) {
        return srnArticleRepository.findByNumber(number);
    }

    @Override
    public List<SrnArticle> getAll() {
        return srnArticleRepository.findAll();
    }

    @Override
    public List<SrnArticle> getAllMaterialIn(List<Integer> materials) {
        return srnArticleRepository.findAllByMaterialIn(materials);
    }
}
