package com.tible.ocm.models;

public enum OcmStatus {

    ACCEPTED("accepted"), DECLINED("declined"), DUPLICATE("duplicate"), FAILED("failed");

    public final String title;

    private OcmStatus(String title) {
        this.title = title;
    }
}
