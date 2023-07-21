package com.tible.ocm.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum MaterialTypeCode {
    NOT_IDENTIFIED("0", 0, "0", 0),
    PET("1", 1, "1", 1),
    GLASS("2", 2, "73", 73),
    STEEL("3", 3, "100", 100),
    ALUMINIUM("4", 4, "41", 41);

    private final String code;
    private final Integer codeInt;
    private final String aaCode;
    private final int aaCodeInt;

    public static List<String> getCodeList() {
        return Arrays.stream(MaterialTypeCode.values())
                .map(MaterialTypeCode::getCode)
                .collect(Collectors.toList());
    }

    public static List<String> getAACodeList() {
        return Arrays.stream(MaterialTypeCode.values())
                .map(MaterialTypeCode::getAaCode)
                .collect(Collectors.toList());
    }

    public static Optional<MaterialTypeCode> getMaterialTypeByAACodeInt(int aaCodeInteger) {
        return Arrays.stream(MaterialTypeCode.values())
                .filter(materialTypeCode -> materialTypeCode.getAaCodeInt() == aaCodeInteger)
                .findFirst();
    }
}
