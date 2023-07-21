package com.tible.ocm.dto.report;

import com.tible.hawk.core.utils.ImportType;
import lombok.Builder;
import lombok.Data;

import java.util.Scanner;

@Data
@Builder
public class HLZHeader {
    private String fortRunningNumber;
    private String identifierOfMessageType;
    private String messageVersionNumber;
    private String dateOfCreation;
    private String glnFirstDistributer;
    private String glnServiceProvider;
    private String constant;
    private ImportType importType;

    public static HLZHeader header(Scanner scanner, ImportType importType) {
        return HLZHeader.builder()
                .fortRunningNumber(scanner.next())
                .identifierOfMessageType(scanner.next())
                .messageVersionNumber(scanner.next())
                .dateOfCreation(scanner.next())
                .glnFirstDistributer(scanner.next())
                .glnServiceProvider(scanner.next())
                .constant(scanner.next())
                .importType(importType)
                .build();
    }
}
