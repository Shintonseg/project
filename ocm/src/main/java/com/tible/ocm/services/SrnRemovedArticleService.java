package com.tible.ocm.services;

import com.tible.ocm.dto.SrnArticles;
import com.tible.ocm.dto.SrnRemovedArticleDto;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.SrnRemovedArticle;

import java.util.List;

public interface SrnRemovedArticleService {

    OcmResponse saveSrnRemovedArticles(List<SrnRemovedArticle> srnArticles);

    List<SrnRemovedArticle> findAll ();

    SrnArticles<SrnRemovedArticleDto> findAll(String version);

    void deleteAll (List<SrnRemovedArticle> deletingArticles);


}
