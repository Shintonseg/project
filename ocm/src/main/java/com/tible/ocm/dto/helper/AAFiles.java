package com.tible.ocm.dto.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AAFiles {

    private Path readyPath;
    private Path readyHashPath;
    private Path batchPath;
    private Path batchHashPath;
    private Path slsPath;
    private Path slsHashPath;
    private Path nlsPath;
    private Path nlsHashPath;
    private Path errorFile;
}
