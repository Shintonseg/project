package com.tible.ocm.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.models.mongo.TransactionArticle;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class TransactionDto {

    private String id;
    private String version;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime dateTime;
    private String storeId;
    private String serialNumber;
    private String transactionNumber;
    private Integer total;
    private Integer refundable;
    private Integer collected;
    private Integer manual;
    private Integer rejected;
    private LocalDateTime receivedDate;
    private String number;
    private String bagType;
    private List<TransactionArticleDto> articles;
    private String charityNumber;

    public TransactionDto(Transaction transaction, List<TransactionArticle> transactionArticles) {
        this.id = transaction.getId();
        this.version = transaction.getVersion();
        this.dateTime = transaction.getDateTime();
        this.storeId = transaction.getStoreId();
        this.serialNumber = transaction.getSerialNumber();
        this.transactionNumber = transaction.getTransactionNumber();
        this.total = transaction.getTotal();
        this.refundable = transaction.getRefundable();
        this.collected = transaction.getCollected();
        this.manual = transaction.getManual();
        this.rejected = transaction.getRejected();
        this.receivedDate = transaction.getReceivedDate();
        this.number = transaction.getLabelNumber();
        this.bagType = transaction.getBagType();
        this.articles = transactionArticles.stream().map(TransactionArticleDto::from).collect(Collectors.toList());
        this.charityNumber = transaction.getCharityNumber();
    }

    public TransactionDto(Transaction transaction) {
        this.id = transaction.getId();
        this.version = transaction.getVersion();
        this.dateTime = transaction.getDateTime();
        this.storeId = transaction.getStoreId();
        this.serialNumber = transaction.getSerialNumber();
        this.transactionNumber = transaction.getTransactionNumber();
        this.total = transaction.getTotal();
        this.refundable = transaction.getRefundable();
        this.collected = transaction.getCollected();
        this.manual = transaction.getManual();
        this.rejected = transaction.getRejected();
        this.receivedDate = transaction.getReceivedDate();
        this.number = transaction.getLabelNumber();
        this.bagType = transaction.getBagType();
        this.charityNumber = transaction.getCharityNumber();
    }

    public static TransactionDto from(Transaction transaction) {
        return transaction == null ? null : new TransactionDto(transaction);
    }

    public static TransactionDto from(Transaction transaction, List<TransactionArticle> transactionArticles) {
        return transaction == null ? null : new TransactionDto(transaction, transactionArticles);
    }

    public Transaction toEntity(MongoTemplate mongoTemplate) {
        Transaction transaction = this.id != null ? mongoTemplate.findById(this.id, Transaction.class) : new Transaction();
        transaction = transaction != null ? transaction : new Transaction();

        transaction.setVersion(this.version);
        transaction.setDateTime(this.dateTime);
        transaction.setStoreId(this.storeId);
        transaction.setSerialNumber(this.serialNumber);
        transaction.setTransactionNumber(this.transactionNumber);
        transaction.setTotal(this.total);
        transaction.setRefundable(this.refundable);
        transaction.setCollected(this.collected);
        transaction.setManual(this.manual);
        transaction.setRejected(this.rejected);
        transaction.setReceivedDate(this.receivedDate);
        transaction.setLabelNumber(this.number);
        transaction.setBagType(this.bagType);
        transaction.setCharityNumber(this.charityNumber);
        // Exported boolean is set by the tasks.
        // transaction.setArticles(this.articles != null ? this.articles.stream().map(transactionArticleDto -> transactionArticleDto.toEntity(mongoTemplate)).collect(Collectors.toList()) : null);

        return transaction;
    }
}
