package com.tible.ocm.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmVersion;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

import static com.tible.ocm.models.OcmVersion.*;
import static com.tible.ocm.utils.ImportHelper.*;

@Slf4j
public class ImportedFileValidationHelper {

    private static final String DETAIL_DESCRIPTION = "One or more import checks failed for the file(s) ";
    private static final String DETAIL_DESCRIPTION_REST = "One or more import checks failed for the transaction received via REST ";

    private ImportedFileValidationHelper() {
    }

    public static List<ImportMessage> buildImportMessages(String message) {
        return List.of(new ImportMessage(0, message));
    }

    public static void createErrorFile(String fileName, Path errorFile, List<ImportMessage> failedChecks) {
        JsonArray list = new JsonArray();
        failedChecks.forEach(failedCheck -> {
            JsonObject failedCheckJsonObject = new JsonObject();
            failedCheckJsonObject.addProperty("lineNumber", failedCheck.getLineNumber());
            failedCheckJsonObject.addProperty("message", failedCheck.getMessage());

            list.add(failedCheckJsonObject);
        });

        JsonObject content = new JsonObject();
        content.addProperty("details", getErrorDetailsMessage(fileName));
        content.add("importMessages", list);

        try (BufferedWriter writer = Files.newBufferedWriter(errorFile,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content.toString());
        } catch (IOException e) {
            log.error("Error creating file");
        }
    }

    public static void createErrorFileForREST(String transactionNumber, Path errorFile, List<OcmMessage> messages, TransactionDto transactionDto) {
        JsonArray list = new JsonArray();
        messages.forEach(message -> {
            JsonObject failedCheckJsonObject = new JsonObject();
            failedCheckJsonObject.addProperty("message", message.getText());

            list.add(failedCheckJsonObject);
        });

        JsonObject content = new JsonObject();
        content.addProperty("details", getErrorDetailsMessageREST(transactionNumber));
        content.add("importMessages", list);
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            content.addProperty("request", objectMapper.writeValueAsString(transactionDto));
        } catch (IOException e) {
            log.warn("Warning creating error file, handling request", e);
        }

        if (!Files.exists(errorFile.getParent())) {
            try {
                Files.createDirectories(errorFile.getParent());
            } catch (IOException e) {
                log.error("Error creating error dir for REST", e);
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(errorFile,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content.toString());
        } catch (IOException e) {
            log.error("Error creating error file for REST", e);
        }
    }

    public static void processWorkWithErrorFile(Path companyIpPath, String subDirectory,
                                                boolean moveFailedToCompanyRejectedDirectory, Path rejectedCompany,
                                                Path... files) {
        if (moveFailedToCompanyRejectedDirectory) {
            copyIfExists(rejectedCompany, files);
            moveToRejectedCompanyDirectory(companyIpPath, subDirectory, files); // subDirectory is TRANS directory
        } else {
            moveIfExists(rejectedCompany, files);
        }
    }

    private static void moveToRejectedCompanyDirectory(Path companyIpPath, String subDirectory, Path... files) {
        Path companyIpTransRejectedPath = companyIpPath.resolve(subDirectory).resolve(REJECTED_DIRECTORY); // subDirectory is TRANS directory
        OcmFileUtils.checkOrCreateDirWithFullPermissions(companyIpTransRejectedPath);
        moveIfExists(companyIpTransRejectedPath, files);
    }

    public static boolean version15Check(String version) {
        return OcmVersion.valueOfTitle(version) != null && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number != null
                && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number >= VERSION_15.number;
    }

    public static boolean version16Check(String version) {
        return OcmVersion.valueOfTitle(version) != null && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number != null
                && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number >= VERSION_16.number;
    }

    public static boolean version162Check(String version) {
        return OcmVersion.valueOfTitle(version) != null && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number != null
                && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number >= VERSION_162.number;
    }

    public static boolean version17Check(String version) {
        return OcmVersion.valueOfTitle(version) != null && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number != null
                && Objects.requireNonNull(OcmVersion.valueOfTitle(version)).number >= VERSION_17.number;
    }

    public static String getErrorDetailsMessage(String fileName) {
        return DETAIL_DESCRIPTION + fileName;
    }

    public static String getErrorDetailsMessageREST(String transactionNumber) {
        return DETAIL_DESCRIPTION_REST + transactionNumber;
    }
}
