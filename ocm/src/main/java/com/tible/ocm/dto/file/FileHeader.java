package com.tible.ocm.dto.file;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Scanner;

@Data
@SuperBuilder
public class FileHeader {
    private String version;
    private String dateTime;

    public static FileHeader header(Scanner scanner) {
        return FileHeader.builder()
                .version(scanner.next())
                .dateTime(scanner.next())
                .build();
    }
}
