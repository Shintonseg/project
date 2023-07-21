package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.SrnArticle;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SrnArticleDtoVersion016 extends SrnArticleDto {

    private String shapeIdentifier;
    private Integer depositCode;
    private String firstArticleActivationDate;
    private String color;

    public SrnArticleDtoVersion016(SrnArticle srnArticle) {
        super(srnArticle);
        this.shapeIdentifier = srnArticle.getShapeIdentifier();
        this.depositCode = srnArticle.getDepositCode();
        this.firstArticleActivationDate = srnArticle.getFirstArticleActivationDate() != null ? srnArticle.getFirstArticleActivationDate().format(DATETIMEFORMATTER) : "";
        this.color = srnArticle.getColor();
    }

    public static SrnArticleDtoVersion016 from(SrnArticle srnArticle) {
        return srnArticle == null ? null : new SrnArticleDtoVersion016(srnArticle);
    }

    @Override
    public SrnArticle toEntity(MongoTemplate mongoTemplate) {
        SrnArticle srnArticle = super.toEntity(mongoTemplate);

        srnArticle.setShapeIdentifier(this.shapeIdentifier);
        srnArticle.setDepositCode(this.depositCode);
        srnArticle.setFirstArticleActivationDate(!StringUtils.isEmpty(this.firstArticleActivationDate) ? LocalDateTime.parse(this.firstArticleActivationDate, DATETIMEFORMATTER) : null);
        srnArticle.setColor(this.color);

        return srnArticle;
    }
}
