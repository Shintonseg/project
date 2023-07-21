package com.tible.ocm.dto.log;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.Map;

@Data
@Builder
public class LogFileInfo implements LogInfo {
    private Path path;
    private String fileName;
    private Map<String, Object> content;
    private boolean isNeedExport;
}
