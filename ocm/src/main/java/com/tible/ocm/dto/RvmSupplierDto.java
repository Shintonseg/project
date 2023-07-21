package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.RvmSupplier;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class RvmSupplierDto {

    private String id;
    private String name;
    private String number;
    private String version;
    private String ftpHostname;
    private String ftpUsername;
    private String ftpPassword;
    private String ipRange;
    private boolean isTrunkIp;
    private List<TransactionDto> transactions;
    private List<RvmMachineDto> rvmMachines;
    private List<RefundArticleDto> refundArticles;

    public RvmSupplierDto(RvmSupplier rvmSupplier) {
        this.id = rvmSupplier.getId();
        this.name = rvmSupplier.getName();
        this.number = rvmSupplier.getNumber();
        this.version = rvmSupplier.getVersion();
        this.ftpHostname = rvmSupplier.getFtpHostname();
        this.ftpUsername = rvmSupplier.getFtpUsername();
        this.ftpPassword = rvmSupplier.getFtpPassword();
        this.ipRange = rvmSupplier.getIpRange();
        this.isTrunkIp = rvmSupplier.isTrunkIp();
        this.transactions = rvmSupplier.getTransactions().stream().map(TransactionDto::from).collect(Collectors.toList());
        this.rvmMachines = rvmSupplier.getRvmMachines().stream().map(RvmMachineDto::from).collect(Collectors.toList());
        this.refundArticles = rvmSupplier.getRefundArticles().stream().map(RefundArticleDto::from).collect(Collectors.toList());
    }

    public static RvmSupplierDto from(RvmSupplier rvmSupplier) {
        return rvmSupplier == null ? null : new RvmSupplierDto(rvmSupplier);
    }

    public RvmSupplier toEntity(MongoTemplate mongoTemplate) {
        RvmSupplier rvmSupplier = this.id != null ? mongoTemplate.findById(this.id, RvmSupplier.class) : new RvmSupplier();
        rvmSupplier = rvmSupplier != null ? rvmSupplier : new RvmSupplier();

        rvmSupplier.setName(this.name);
        rvmSupplier.setNumber(this.number);
        rvmSupplier.setVersion(this.version);
        rvmSupplier.setFtpHostname(this.ftpHostname);
        rvmSupplier.setFtpUsername(this.ftpUsername);
        rvmSupplier.setFtpPassword(this.ftpPassword);
        rvmSupplier.setIpRange(this.ipRange);
        rvmSupplier.setTrunkIp(this.isTrunkIp());
        rvmSupplier.setTransactions(this.transactions != null ? this.transactions.stream()
                .map(transactionDto -> transactionDto.toEntity(mongoTemplate)).collect(Collectors.toList()) : null);
        rvmSupplier.setRvmMachines(this.rvmMachines != null ? this.rvmMachines.stream()
                .map(rvmMachineDto -> rvmMachineDto.toEntity(mongoTemplate)).collect(Collectors.toList()) : null);
        rvmSupplier.setRefundArticles(this.refundArticles != null ? this.refundArticles.stream()
                .map(refundArticleDto -> refundArticleDto.toEntity(mongoTemplate)).collect(Collectors.toList()) : null);

        return rvmSupplier;
    }
}
