package com.tible.ocm.configurations;

import com.tible.ocm.models.RvmSupplierYml;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "rvm-suppliers")
public class RvmSuppliersConfiguration {
    private List<RvmSupplierYml> rvmSupplierYmlList = new ArrayList<>();

    public List<RvmSupplierYml> getRvmSupplierYmlList() {
        return rvmSupplierYmlList;
    }

    public void setRvmSupplierYmlList(List<RvmSupplierYml> rvmSupplierYmlList) {
        this.rvmSupplierYmlList = rvmSupplierYmlList;
    }
}
