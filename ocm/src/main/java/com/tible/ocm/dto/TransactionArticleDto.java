package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.TransactionArticle;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

@Data
@NoArgsConstructor
public class TransactionArticleDto {

    private String id;
    private String articleNumber;
    private Integer scannedWeight;
    private Integer material;
    private Integer refund;
    private Integer collected;
    private Integer manual;

    public TransactionArticleDto(TransactionArticle transactionArticle) {
        this.id = transactionArticle.getId();
        this.articleNumber = transactionArticle.getArticleNumber();
        this.scannedWeight = transactionArticle.getScannedWeight();
        this.material = transactionArticle.getMaterial();
        this.refund = transactionArticle.getRefund();
        this.collected = transactionArticle.getCollected();
        this.manual = transactionArticle.getManual();
    }

    public static TransactionArticleDto from(TransactionArticle transactionArticle) {
        return transactionArticle == null ? null : new TransactionArticleDto(transactionArticle);
    }

    public TransactionArticle toEntity(MongoTemplate mongoTemplate) {
        TransactionArticle transactionArticle = this.id != null ? mongoTemplate.findById(this.id, TransactionArticle.class) : new TransactionArticle();
        transactionArticle = transactionArticle != null ? transactionArticle : new TransactionArticle();

        transactionArticle.setArticleNumber(this.articleNumber);
        transactionArticle.setScannedWeight(this.scannedWeight);
        transactionArticle.setMaterial(this.material);
        transactionArticle.setRefund(this.refund);
        transactionArticle.setCollected(this.collected);
        transactionArticle.setManual(this.manual);

        return transactionArticle;
    }
}
