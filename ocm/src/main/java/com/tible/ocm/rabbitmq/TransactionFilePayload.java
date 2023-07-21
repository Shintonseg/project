package com.tible.ocm.rabbitmq;

public class TransactionFilePayload {

    private String name;
    private String companyId;
    private String type;

    public TransactionFilePayload() {
    }

    public TransactionFilePayload(String name, String companyId, String type) {
        this.name = name;
        this.companyId = companyId;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TransactionFilePayload{" +
                "name='" + name + '\'' +
                ", companyId='" + companyId + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
