package com.tible.ocm.dto.file;

import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.Builder;
import lombok.Data;

import java.util.Scanner;

@Data
@Builder
public class TransactionBody {
    private String articleNumber;
    private String scannedWeight;
    private String material;
    private String refunded;
    private String collected;
    private String manual;
    private ImportType importType;

    public static TransactionBody body(Scanner scanner, String version, ImportType importType) {
        // if version 162 has different fields, than version 162 can be used as the if and version 15 as else if. Else part is used for older versions.
        if (ImportedFileValidationHelper.version162Check(version)) {
            return TransactionBody.builder()
                    .articleNumber(scanner.next())
                    .scannedWeight(scanner.next())
                    .material(scanner.next())
                    .refunded(scanner.next())
                    .collected(scanner.next())
                    .manual(scanner.next())
                    .importType(importType)
                    .build();
        } else if (ImportedFileValidationHelper.version15Check(version)) {
            return TransactionBody.builder()
                    .articleNumber(scanner.next())
                    .scannedWeight(scanner.next())
                    .material(scanner.next())
                    .refunded(scanner.next())
                    .collected(scanner.next())
                    .importType(importType)
                    .build();
        } else {
            return TransactionBody.builder()
                    .articleNumber(scanner.next())
                    .scannedWeight(scanner.next())
                    .refunded(scanner.next())
                    .collected(scanner.next())
                    .importType(importType)
                    .build();
        }
    }
}
