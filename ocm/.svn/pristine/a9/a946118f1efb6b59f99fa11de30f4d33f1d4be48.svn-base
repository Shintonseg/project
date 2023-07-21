package com.tible.ocm.models.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class RvmMachine {

    @Id
    private String id;
    private String serialNumber;
    private String storeId;
    @Indexed(unique = true)
    private String ipAddress;
    private String version;

}
