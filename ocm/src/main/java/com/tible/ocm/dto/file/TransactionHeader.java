package com.tible.ocm.dto.file;

import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Scanner;

@Getter
@Setter
@SuperBuilder
public class TransactionHeader extends FileHeader {
    private String storeId;
    private String rvmSerial;
    private String labelNumber;
    private String bagType;
    private String charityNumber;
    private ImportType importType;

    public static TransactionHeader header(Scanner scanner, String version, ImportType importType) {
        // if version 162 has different fields, than version 162 can be used as the if and version 15 as else if. Else part is used forolder versions.
        if (ImportedFileValidationHelper.version17Check(version)) {
            return TransactionHeader.builder()
                    .version(scanner.next())
                    .dateTime(scanner.next())
                    .storeId(scanner.next())
                    .rvmSerial(scanner.next())
                    .labelNumber(scanner.next())
                    .bagType(scanner.next())
                    .charityNumber(scanner.next())
                    .importType(importType)
                    .build();
        } else if (ImportedFileValidationHelper.version162Check(version)) {
            return TransactionHeader.builder()
                    .version(scanner.next())
                    .dateTime(scanner.next())
                    .storeId(scanner.next())
                    .rvmSerial(scanner.next())
                    .labelNumber(scanner.next())
                    .bagType(scanner.next())
                    .importType(importType)
                    .build();
        } else {
            return TransactionHeader.builder()
                    .version(scanner.next())
                    .dateTime(scanner.next())
                    .storeId(scanner.next())
                    .rvmSerial(scanner.next())
                    .importType(importType)
                    .build();
        }
    }
}
