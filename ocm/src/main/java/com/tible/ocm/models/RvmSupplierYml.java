package com.tible.ocm.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RvmSupplierYml {

    private String name;
    private String number;
    private String ip;
    private String username;
    private String password;
    private String version;
    private String storeId;

}
