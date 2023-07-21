package com.tible.ocm.dto.file;

import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.Builder;
import lombok.Data;

import java.util.Scanner;

@Data
@Builder
public class FileFooter {
    private String total;

    // TODO: Split these to sub class
    private String refunded;
    private String collected;
    private String manual;
    private String rejected;

    private ImportType importType;

    public static FileFooter footer(Scanner scanner, String version, ImportType importType) {
        // if version 162 has different fields, than version 162 can be used as the if and version 15 as else if. Else part is used for older versions.
        if (ImportedFileValidationHelper.version162Check(version)) {
            return FileFooter.builder()
                    .total(scanner.next())
                    .refunded(scanner.next())
                    .collected(scanner.next())
                    .manual(scanner.next())
                    .rejected(scanner.next())
                    .importType(importType)
                    .build();
        } else {
            return FileFooter.builder()
                    .total(scanner.next())
                    .refunded(scanner.next())
                    .collected(scanner.next())
                    .importType(importType)
                    .build();
        }
    }
}
