package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.SrnRemovedArticle;
import lombok.Data;

import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

@Data
public class SrnRemovedArticleDto {

    String number;
    String deactivationDate;

    public SrnRemovedArticleDto(SrnRemovedArticle article) {
        this.number = article.getNumber();
        this.deactivationDate = article.getDeactivationDate() != null ?
                article.getDeactivationDate().format(DATETIMEFORMATTER) : "";
    }
}
