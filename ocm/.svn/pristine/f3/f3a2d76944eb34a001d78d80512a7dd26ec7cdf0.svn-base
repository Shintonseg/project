package com.tible.ocm.services;

import com.tible.ocm.dto.RefundArticles;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.models.mongo.RefundArticle;

import java.util.List;

public interface RefundArticleService {

    RefundArticles findAll();

    OcmResponse saveRefundArticles(RefundArticles refundArticles, String ipAddress);

    List<RefundArticle> findAllByCompanyId(String companyId);

}
