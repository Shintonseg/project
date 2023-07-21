package com.tible.ocm.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class RvmSupplier {

    @Id
    private String id;
    private String name;
    private String number;
    private String version;
    private String ftpHostname;
    private String ftpUsername;
    private String ftpPassword;
    private String ipRange;
    private boolean isTrunkIp;

    @DBRef
    private List<Transaction> transactions;
    @DBRef
    private List<RvmMachine> rvmMachines;
    @DBRef
    private List<RefundArticle> refundArticles;

}
