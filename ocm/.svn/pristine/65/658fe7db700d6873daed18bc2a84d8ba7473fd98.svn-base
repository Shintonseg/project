package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.SrnArticle;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SrnArticleDtoVersion017 extends SrnArticleDtoVersion016 {

    public SrnArticleDtoVersion017(SrnArticle srnArticle) {
        super(srnArticle);
    }

    public static SrnArticleDtoVersion017 from(SrnArticle srnArticle) {
        return srnArticle == null ? null : new SrnArticleDtoVersion017(srnArticle);
    }

    @Override
    public SrnArticle toEntity(MongoTemplate mongoTemplate) {
        SrnArticle srnArticle = super.toEntity(mongoTemplate);

        return srnArticle;
    }
}
