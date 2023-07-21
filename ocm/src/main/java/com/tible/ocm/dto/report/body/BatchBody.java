package com.tible.ocm.dto.report.body;

import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.report.Body;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Scanner;

@Data
@Builder
public class BatchBody implements Body {
    private String batchId;
    private String referenceNumber;
    private String user;
    private String batchTimeStart;
    private String numberOfRefundable;
    private String numberOfRefundableFromNoReadTable;
    private String numberOfNonRefundable;
    private String numberOfNonRefundableFromNoReadTable;
    private String eanNotReadable;
    private String unknown;
    private String portZero; // or metal counter if you have version 2019-11-2106, ver.2 of How to understand HLZ file formats pdf
    private String numberInBatch;
    private String numberInShift;
    private String batchTimeEnd;
    private String firstBlankReservedField;
    private String batchTime;
    private String keyId;
    private String ankerAndersenILNNumber;
    private String pricatVersion;
    private String trashCodes;
    private String secondBlankReservedField;
    private String thirdBlankReservedField;
    private String fourthBlankReservedField;
    private String totalDepositAmount;
    private String reserved;
    private ImportType importType;

    public static BatchBody body(Pair<Scanner, ImportType> scannerImportTypePair) {
        Scanner scanner = scannerImportTypePair.getLeft();
        ImportType importType = scannerImportTypePair.getRight();

        return BatchBody.builder()
                .batchId(scanner.next())
                .referenceNumber(scanner.next())
                .user(scanner.next())
                .batchTimeStart(scanner.next())
                .numberOfRefundable(scanner.next())
                .numberOfRefundableFromNoReadTable(scanner.next())
                .numberOfNonRefundable(scanner.next())
                .numberOfNonRefundableFromNoReadTable(scanner.next())
                .eanNotReadable(scanner.next())
                .unknown(scanner.next())
                .portZero(scanner.next())
                .numberInBatch(scanner.next())
                .numberInShift(scanner.next())
                .batchTimeEnd(scanner.next())
                .firstBlankReservedField(scanner.next())
                .batchTime(scanner.next())
                .keyId(scanner.next())
                .ankerAndersenILNNumber(scanner.next())
                .pricatVersion(scanner.next())
                .trashCodes(scanner.next())
                .secondBlankReservedField(scanner.next())
                .thirdBlankReservedField(scanner.next())
                .fourthBlankReservedField(scanner.next())
                .totalDepositAmount(scanner.next())
                .reserved(scanner.next())
                .importType(importType)
                .build();
    }

}
