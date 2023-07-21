package com.tible.ocm.services.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tible.ocm.services.log.LogKeyConstant.*;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportedFileValidationHelper.getErrorDetailsMessage;

@Slf4j
@Component
public class LogFileExporterServiceImpl implements LogExporterService<LogFileInfo> {

    private final DirectoryService directoryService;
    private final ObjectMapper objectMapper;

    @Autowired
    public LogFileExporterServiceImpl(DirectoryService directoryService, ObjectMapper objectMapper) {
        this.directoryService = directoryService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getLogPath())) {
            log.error("Creating log directory failed");
        }
    }

    @Override
    @SneakyThrows
    public void export(LogFileInfo info) {
        if(!info.isNeedExport()) {
            return;
        }
        String fileNameWithType = getFileNameWithType(info.getFileName());
        Path resolvePath = info.getPath().resolve(fileNameWithType);
        Map<String, Object> content = info.getContent();
        String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(content);

        if (OcmFileUtils.checkOrCreateDirWithFullPermissions(info.getPath())) {
            try (BufferedWriter writer = Files.newBufferedWriter(resolvePath,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                writer.write(jsonContent);
            } catch (IOException e) {
                log.error("Error creating file", e);
            }
        }
    }

    @Override
    public void exportWithDetailMessage(String key, List<String> logList, String message, LogFileInfo info) {
        Map<String, Object> logContent = new HashMap<>();
        logContent.put(key, logList);
        logContent.put(DETAILS_KEY, message);
        logContent.put(CREATED_DATETIME_KEY, LocalDateTime.now());
        info.setContent(logContent);
        export(info);
    }

    @Override
    public void exportWithContentMap(Path file, Map<String, Object> contentMap, List<ImportMessage> importMessages,
                                     LogFileInfo logFileInfo, Company company, boolean alreadyExists, String status) {
        if (importMessages != null && importMessages.size() > 0) {
            contentMap.put(DETAILS_KEY, getErrorDetailsMessage(getFilename(file)));
            if (StringUtils.isNotEmpty(status) && status.equals(IMPORT_STATUS_FAILED)) {
                contentMap.put(IMPORT_FAILED_MESSAGES_KEY, importMessages);
            } else {
                contentMap.put(IMPORT_MESSAGES_KEY, importMessages);
            }
        } else if (alreadyExists) {
            contentMap.put(DETAILS_KEY, "File(s) of " + getFilename(file) + " were moved to already exists directory");
        } else {
            contentMap.put(DETAILS_KEY, "File(s) of " + getFilename(file) + " were handled successfully");
        }
        if (company != null) {
            contentMap.put(COMPANY_NUMBER_KEY, company.getNumber());
            contentMap.put(IP_ADDRESS_KEY, company.getIpAddress());
        }
        if (StringUtils.isNotEmpty(status)) {
            contentMap.put(IMPORT_STATUS_KEY, status);
        }
        logFileInfo.setNeedExport(true);
        logToFile(logFileInfo, contentMap);
    }

    @Override
    public void exportWithContentMap(Map<String, Object> contentMap, List<OcmMessage> messages, String transactionNumber,
                                     LogFileInfo logFileInfo, Company company, boolean alreadyExists, String status) {
        if (messages != null && messages.size() > 0) {
            contentMap.put(DETAILS_KEY, "One or more import checks failed for the transaction " + transactionNumber);
            if (StringUtils.isNotEmpty(status) && status.equals(IMPORT_STATUS_FAILED)) {
                contentMap.put(IMPORT_FAILED_MESSAGES_KEY, messages);
            } else {
                contentMap.put(IMPORT_MESSAGES_KEY, messages);
            }
        } else if (alreadyExists) {
            contentMap.put(DETAILS_KEY, "Transaction " + transactionNumber + " already exists");
        } else {
            contentMap.put(DETAILS_KEY, "Transaction " + transactionNumber + " was handled successfully");
        }
        if (company != null) {
            contentMap.put(COMPANY_NUMBER_KEY, company.getNumber());
            contentMap.put(IP_ADDRESS_KEY, company.getIpAddress());
        }
        if (StringUtils.isNotEmpty(status)) {
            contentMap.put(IMPORT_STATUS_KEY, status);
        }
        logFileInfo.setNeedExport(true);
        logToFile(logFileInfo, contentMap);
    }

    @Override
    public void logToFile(LogFileInfo logFileInfo, Map<String, Object> contentMap) {
        contentMap.put(CREATED_DATETIME_KEY, LocalDateTime.now());
        logFileInfo.setContent(contentMap);
        export(logFileInfo);
    }

    private String getFileNameWithType(String fileName) {
        return fileName + LOG_FILE_FORMAT;
    }
}
