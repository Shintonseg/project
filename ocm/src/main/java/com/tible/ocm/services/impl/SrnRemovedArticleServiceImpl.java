package com.tible.ocm.services.impl;

import com.tible.ocm.dto.SrnArticles;
import com.tible.ocm.dto.SrnRemovedArticleDto;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.SrnRemovedArticle;
import com.tible.ocm.repositories.mongo.SrnRemovedArticleRepository;
import com.tible.ocm.services.SrnRemovedArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.tible.ocm.models.OcmStatus.ACCEPTED;

@Slf4j
@Primary
@Service
public class SrnRemovedArticleServiceImpl implements SrnRemovedArticleService {

    private final SrnRemovedArticleRepository srnRemovedArticleRepository;

    public SrnRemovedArticleServiceImpl(SrnRemovedArticleRepository srnRemovedArticleRepository) {
        this.srnRemovedArticleRepository = srnRemovedArticleRepository;
    }

    @Override
    public OcmResponse saveSrnRemovedArticles(List<SrnRemovedArticle> srnRemovedArticles) {
        for (SrnRemovedArticle srnRemovedArticle : srnRemovedArticles) {
            SrnRemovedArticle existingArticle = srnRemovedArticleRepository.findByNumber(srnRemovedArticle.getNumber());
            if (existingArticle == null) {
                srnRemovedArticle.setCreatedDateTime(LocalDateTime.now());
                srnRemovedArticleRepository.save(srnRemovedArticle);
            } else {
                srnRemovedArticleRepository.save(fillArticle(existingArticle, srnRemovedArticle));
            }
        }
        log.info("Saved or updated {} SRN removed articles: {}", srnRemovedArticles.size(),
                srnRemovedArticles.stream().map(SrnRemovedArticle::getNumber).collect(Collectors.joining(",")));
        return OcmResponse.builder().status(ACCEPTED).build();
    }

    @Override
    public List<SrnRemovedArticle> findAll() {
        return srnRemovedArticleRepository.findAll();
    }

    @Override
    public SrnArticles<SrnRemovedArticleDto> findAll(String version) {
        List<SrnRemovedArticle> articles = srnRemovedArticleRepository.findAll();
        SrnArticles<SrnRemovedArticleDto> srnArticles = new SrnArticles<>();
        srnArticles.setArticles(articles.stream().map(SrnRemovedArticleDto::new).collect(Collectors.toList()));
        srnArticles.setVersion(version);
        srnArticles.setDateTime(LocalDateTime.now());
        srnArticles.setTotal(articles.size());
        return srnArticles;
    }

    @Override
    public void deleteAll(List<SrnRemovedArticle> deletingArticles) {
        srnRemovedArticleRepository.deleteAll(deletingArticles);
    }

    private SrnRemovedArticle fillArticle (SrnRemovedArticle existingArticle, SrnRemovedArticle article) {
        existingArticle.setNumber(article.getNumber());
        existingArticle.setDeactivationDate(article.getDeactivationDate());
        return existingArticle;
    }
}
