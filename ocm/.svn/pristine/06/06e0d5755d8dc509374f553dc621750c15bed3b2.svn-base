package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.RefundArticle;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RefundArticleDto {

    private String id;
    private String number;
    private String supplier;
    private LocalDateTime activationDate;
    private Integer weightMin;
    private Integer weightMax;
    private Integer volume;
    private Integer height;
    private Integer diameter;
    private Integer material;
    private Integer type;
    private String description;
    private Integer wildcard;

    public RefundArticleDto(RefundArticle refundArticle) {
        this.id = refundArticle.getId();
        this.number = refundArticle.getNumber();
        this.supplier = refundArticle.getSupplier();
        this.activationDate = refundArticle.getActivationDate();
        this.weightMin = refundArticle.getWeightMin();
        this.weightMax = refundArticle.getWeightMax();
        this.volume = refundArticle.getVolume();
        this.height = refundArticle.getHeight();
        this.diameter = refundArticle.getDiameter();
        this.material = refundArticle.getMaterial();
        this.type = refundArticle.getType();
        this.description = refundArticle.getDescription();
        this.wildcard = refundArticle.getWildcard();
    }

    public static RefundArticleDto from(RefundArticle refundArticle) {
        return refundArticle == null ? null : new RefundArticleDto(refundArticle);
    }

    public RefundArticle toEntity(MongoTemplate mongoTemplate) {
        RefundArticle refundArticle = this.id != null ? mongoTemplate.findById(this.id, RefundArticle.class) : new RefundArticle();
        refundArticle = refundArticle != null ? refundArticle : new RefundArticle();

        refundArticle.setNumber(this.number);
        refundArticle.setSupplier(this.supplier);
        refundArticle.setActivationDate(this.activationDate);
        refundArticle.setWeightMin(this.weightMax);
        refundArticle.setWeightMax(this.weightMax);
        refundArticle.setVolume(this.volume);
        refundArticle.setHeight(this.height);
        refundArticle.setDiameter(this.diameter);
        refundArticle.setMaterial(this.material);
        refundArticle.setType(this.type);
        refundArticle.setDescription(this.description);
        refundArticle.setWildcard(this.wildcard);

        return refundArticle;
    }
}
