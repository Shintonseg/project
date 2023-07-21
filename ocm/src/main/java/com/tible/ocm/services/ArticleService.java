package com.tible.ocm.services;

import com.tible.ocm.models.mongo.RefundArticle;

import java.nio.file.Path;
import java.util.List;

public interface ArticleService {

    void processArticleFile(String number, String version, List<RefundArticle> refundArticles, String ipAddress,
                            Path file, boolean moveFailedToCompanyRejectedDirectory,
                            String communication);
}
