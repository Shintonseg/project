package com.tible.ocm.services.log;

import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.dto.log.LogInfo;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.Company;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface LogExporterService<T extends LogInfo> {

    void export(T info);

    void exportWithDetailMessage(String key, List<String> logList, String message, T info);

    void exportWithContentMap(Path file, Map<String, Object> contentMap, List<ImportMessage> importMessages,
                              LogFileInfo logFileInfo, Company company, boolean alreadyExists, String status);

    void exportWithContentMap(Map<String, Object> contentMap, List<OcmMessage> messages, String transactionNumber,
                              LogFileInfo logFileInfo, Company company, boolean alreadyExists, String status);

    void logToFile(LogFileInfo logFileInfo, Map<String, Object> contentMap);

}
