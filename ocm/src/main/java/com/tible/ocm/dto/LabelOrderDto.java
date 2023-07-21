package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.LabelOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class LabelOrderDto {

    private String id;
    private String customerNumber;
    private String customerLocalizationNumber;
    private String rvmOwnerNumber;
    private Long quantity;
    private Long balance;
    private Long firstLabelNumber;
    private Long lastLabelNumber;
    private LocalDateTime orderDate;
    private Boolean markAllLabelsAsUsed;

    public LabelOrderDto(LabelOrder labelOrder) {
        this.id = labelOrder.getId();
        this.customerNumber = labelOrder.getCustomerNumber();
        this.customerLocalizationNumber = labelOrder.getCustomerLocalizationNumber();
        this.rvmOwnerNumber = labelOrder.getRvmOwnerNumber();
        this.quantity = labelOrder.getQuantity();
        this.balance = labelOrder.getBalance();
        this.firstLabelNumber = labelOrder.getFirstLabelNumber();
        this.lastLabelNumber = labelOrder.getLastLabelNumber();
        this.orderDate = labelOrder.getOrderDate();
        this.markAllLabelsAsUsed = labelOrder.getMarkAllLabelsAsUsed();
    }

    public static LabelOrderDto from(LabelOrder labelOrder) {
        return labelOrder == null ? null : new LabelOrderDto(labelOrder);
    }

    public LabelOrder toEntity(MongoTemplate mongoTemplate) {
        LabelOrder labelOrder = this.id != null ? mongoTemplate.findById(this.id, LabelOrder.class) : new LabelOrder();
        labelOrder = labelOrder != null ? labelOrder : new LabelOrder();

        labelOrder.setCustomerNumber(this.customerNumber);
        labelOrder.setCustomerLocalizationNumber(this.customerLocalizationNumber);
        labelOrder.setRvmOwnerNumber(this.rvmOwnerNumber);
        labelOrder.setQuantity(this.quantity);
        labelOrder.setBalance(this.balance);
        labelOrder.setFirstLabelNumber(this.firstLabelNumber);
        labelOrder.setLastLabelNumber(this.lastLabelNumber);
        labelOrder.setOrderDate(this.orderDate);
        labelOrder.setMarkAllLabelsAsUsed(this.markAllLabelsAsUsed);

        return labelOrder;
    }
}
