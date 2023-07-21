package com.tible.ocm.models;

public enum OcmVersion {

    VERSION_12("012", 120),
    VERSION_15("015", 150),
    VERSION_16("016", 160),
    VERSION_162("0162", 162),
    VERSION_17("017", 170),
    VERSION_171("0171", 171);

    public final String title;
    public final Integer number;

    OcmVersion(String title, Integer number) {
        this.title = title;
        this.number = number;
    }

    public static OcmVersion valueOfTitle(String title) {
        for (OcmVersion v : values()) {
            if (v.title.equals(title)) {
                return v;
            }
        }
        return null;
    }

}
