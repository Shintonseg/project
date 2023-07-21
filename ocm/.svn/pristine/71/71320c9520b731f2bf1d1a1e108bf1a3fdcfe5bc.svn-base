package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.SrnArticle;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

@Data
@NoArgsConstructor
public class SrnArticleDto {

    private String id;
    private String number;
    private String supplier;
    private String activationDate;
    private Integer weight;
    private Integer volume;
    private Integer height;
    private Integer diameter;
    private Integer material;
    private Integer depositValue;
    private String description;

    public SrnArticleDto(SrnArticle srnArticle) {
        this.id = srnArticle.getId();
        this.number = srnArticle.getNumber();
        this.supplier = srnArticle.getSupplier();
        this.activationDate = srnArticle.getActivationDate() != null ? srnArticle.getActivationDate().format(DATETIMEFORMATTER) : "";
        this.weight = srnArticle.getWeight();
        this.volume = srnArticle.getVolume();
        this.height = srnArticle.getHeight();
        this.diameter = srnArticle.getDiameter();
        this.material = srnArticle.getMaterial();
        this.depositValue = srnArticle.getDepositValue();
        this.description = srnArticle.getDescription();
    }

    public static SrnArticleDto from(SrnArticle srnArticle) {
        return srnArticle == null ? null : new SrnArticleDto(srnArticle);
    }

    public SrnArticle toEntity(MongoTemplate mongoTemplate) {
        SrnArticle srnArticle = this.id != null ? mongoTemplate.findById(this.id, SrnArticle.class) : new SrnArticle();
        srnArticle = srnArticle != null ? srnArticle : new SrnArticle();

        srnArticle.setNumber(this.number);
        srnArticle.setSupplier(this.supplier);
        srnArticle.setActivationDate(!StringUtils.isEmpty(this.activationDate) ? LocalDateTime.parse(this.activationDate, DATETIMEFORMATTER) : null);
        srnArticle.setWeight(this.weight);
        srnArticle.setVolume(this.volume);
        srnArticle.setHeight(this.height);
        srnArticle.setDiameter(this.diameter);
        srnArticle.setMaterial(this.material);
        srnArticle.setDepositValue(this.depositValue);
        srnArticle.setDescription(this.description);

        return srnArticle;
    }
}
