package com.tible.ocm.dto.report.body;

import com.tible.hawk.core.utils.ImportType;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Scanner;

@Data
@Builder
public class SlsNlsBody {
    private String glnManufacturer;
    private String keyId;
    private String articleNumber;
    private String dateAndTime;
    private String firstBlankReservedField;
    private String secondBlankReservedField;
    private String batchId;
    private String cameraNumber;
    private String firstZeroReservedField;
    private String secondZeroReservedField;
    private String ejectionStationNo;
    private String thirdZeroReservedField;
    private String fourthZeroReservedField;
    private String fifthZeroReservedField;
    private String thirdBlankReservedField;
    private String depositAmount;
    private String typeOfMaterial;
    private String materialTypeDetected;
    private String fourthBlankReservedField;
    private String fifthBlankReservedField;
    private String sixthBlankReservedField;
    private ImportType importType;

    public static SlsNlsBody body(Pair<Scanner, ImportType> scannerImportTypePair) {
        Scanner scanner = scannerImportTypePair.getLeft();
        ImportType importType = scannerImportTypePair.getRight();

        return SlsNlsBody.builder()
                .glnManufacturer(scanner.next())
                .keyId(scanner.next())
                .articleNumber(scanner.next())
                .dateAndTime(scanner.next())
                .firstBlankReservedField(scanner.next())
                .secondBlankReservedField(scanner.next())
                .batchId(scanner.next())
                .cameraNumber(scanner.next())
                .firstZeroReservedField(scanner.next())
                .secondZeroReservedField(scanner.next())
                .ejectionStationNo(scanner.next())
                .thirdZeroReservedField(scanner.next())
                .fourthZeroReservedField(scanner.next())
                .fifthZeroReservedField(scanner.next())
                .thirdBlankReservedField(scanner.next())
                .depositAmount(scanner.next())
                .typeOfMaterial(scanner.next())
                .materialTypeDetected(scanner.next())
                .fourthBlankReservedField(scanner.next())
                .fifthBlankReservedField(scanner.next())
                .sixthBlankReservedField(scanner.hasNext() ? scanner.next() : "")
                .importType(importType)
                .build();
    }
}
