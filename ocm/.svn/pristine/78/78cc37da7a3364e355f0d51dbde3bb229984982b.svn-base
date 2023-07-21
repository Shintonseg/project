package com.tible.ocm.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@ConfigurationPropertiesScan
@EnableConfigurationProperties
public class DefaultClientDirectoriesConfiguration {

    @Value("${sftp-rvm.articles-export-to-dir}")
    private String inDirectory;
    @Value("${sftp-rvm.articles-import-from-dir}")
    private String outDirectory;
    @Value("${sftp-rvm.transactions-dir}")
    private String transDirectory;
    @Value("${sftp-rvm.rejected-dir}")
    private String rejectedDirectory;
    @Value("${sftp-rvm.bags-export-to-dir}")
    private String bagsDirectory;


    private List<String> allInnerDirectories;

    @PostConstruct
    public void init() {
        this.allInnerDirectories = List.of(inDirectory, outDirectory, transDirectory);
    }

    public String getInDirectory() {
        return inDirectory;
    }

    public void setInDirectory(String inDirectory) {
        this.inDirectory = inDirectory;
    }

    public String getOutDirectory() {
        return outDirectory;
    }

    public void setOutDirectory(String outDirectory) {
        this.outDirectory = outDirectory;
    }

    public String getTransDirectory() {
        return transDirectory;
    }

    public void setTransDirectory(String transDirectory) {
        this.transDirectory = transDirectory;
    }

    public String getRejectedDirectory() {
        return rejectedDirectory;
    }

    public void setRejectedDirectory(String rejectedDirectory) {
        this.rejectedDirectory = rejectedDirectory;
    }

    public List<String> getAllInnerDirectories() {
        return allInnerDirectories;
    }

    public String getBagsDirectory() {
        return bagsDirectory;
    }

    public void setBagsDirectory(String bagsDirectory) {
        this.bagsDirectory = bagsDirectory;
    }
}
