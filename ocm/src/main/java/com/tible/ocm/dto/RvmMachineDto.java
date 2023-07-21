package com.tible.ocm.dto;

import com.tible.ocm.models.mongo.RvmMachine;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;

@Data
@NoArgsConstructor
public class RvmMachineDto {

    private String id;
    private String serialNumber;
    private String storeId;
    private String ipAddress;
    private String version;

    public RvmMachineDto(RvmMachine rvmMachine) {
        this.id = rvmMachine.getId();
        this.serialNumber = rvmMachine.getSerialNumber();
        this.storeId = rvmMachine.getStoreId();
        this.ipAddress = rvmMachine.getIpAddress();
        this.version = rvmMachine.getVersion();
    }

    public static RvmMachineDto from(RvmMachine rvmMachine) {
        return rvmMachine == null ? null : new RvmMachineDto(rvmMachine);
    }

    public RvmMachine toEntity(MongoTemplate mongoTemplate) {
        RvmMachine rvmMachine = this.id != null ? mongoTemplate.findById(this.id, RvmMachine.class) : new RvmMachine();
        rvmMachine = rvmMachine != null ? rvmMachine : new RvmMachine();

        rvmMachine.setSerialNumber(this.serialNumber);
        rvmMachine.setStoreId(this.storeId);
        rvmMachine.setIpAddress(this.ipAddress);
        rvmMachine.setVersion(this.version);

        return rvmMachine;
    }
}
