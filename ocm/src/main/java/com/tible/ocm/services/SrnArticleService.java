package com.tible.ocm.services;

import com.tible.ocm.dto.SrnArticleDto;
import com.tible.ocm.dto.SrnArticleDtoVersion016;
import com.tible.ocm.dto.SrnArticleDtoVersion017;
import com.tible.ocm.dto.SrnArticles;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.SrnArticle;

import java.util.List;

public interface SrnArticleService {

    SrnArticles<SrnArticleDto> findAll(String version);

    SrnArticles<SrnArticleDtoVersion016> findAllVersion016(String version);

    List<SrnArticle> findAllArticles();

    OcmResponse saveSrnArticles(List<SrnArticle> srnArticles);

    void deleteSrnArticle(String id);

    void deleteAll(List<SrnArticle> srnArticles);

    boolean existsAndIsActiveByArticleNumberAndRefundable(String number, Integer refund);

    boolean existsByArticleNumber(String number);

    SrnArticle findByArticleNumber(String number);

    List<SrnArticle> getAll();

    List<SrnArticle> getAllMaterialIn(List<Integer> materials);

    SrnArticles<SrnArticleDtoVersion017> findAllVersion017(String version);
}
