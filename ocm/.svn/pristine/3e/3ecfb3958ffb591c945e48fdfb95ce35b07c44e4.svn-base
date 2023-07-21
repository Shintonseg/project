package com.tible.ocm.utils;

import com.tible.hawk.core.controllers.helpers.BaseMessageType;
import com.tible.hawk.core.controllers.helpers.MailData;
import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.file.FileFooter;
import com.tible.ocm.dto.file.FileHeader;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.mongo.Company;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

public class ImportHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportHelper.class);

    public static final String IMPORT_STATUS_ACCEPTED = "ACCEPTED";
    public static final String IMPORT_STATUS_REJECTED = "REJECTED";
    public static final String IMPORT_STATUS_ALREADY_EXISTS = "ALREADY_EXISTS";
    public static final String IMPORT_STATUS_FAILED = "FAILED";

    public static final String HEADER_REGEX = "\"?HDR|POS|SUM\"?";
    public static final DateTimeFormatter READABLE_DATE_PATTERN = DateTimeFormatter.ofPattern("d-M-yyyy HH:mm");
    public static final String TRANS_DIRECTORY = "TRANS";
    public static final String INPUT_DIRECTORY = "IN";
    public static final String OUTPUT_DIRECTORY = "OUT";
    public static final String BAGS_DIRECTORY = "BAGS";
    public static final String REJECTED_DIRECTORY = "rejected";
    public static final String CONFIRMED_DIRECTORY = "confirmed";
    public static final String ARTICLE_EXPORT = "article";
    public static final String ARTICLE_EXPORT_CSV = "article.csv";
    public static final String ARTICLE_EXPORT_HASH = "article.hash";
    public static final String ARTICLE_REMOVED_EXPORT_CSV = "article-removed.csv";
    public static final String ARTICLE_REMOVED_EXPORT_HASH = "article-removed.hash";
    public static final String CHARITIES_EXPORT = "charities";
    public static final String CHARITIES_EXPORT_CSV = "charities.csv";
    public static final String CHARITIES_EXPORT_HASH = "charities.hash";

    // Pricat article files
    public static final String ARTICLES_PRICAT_EXPORT = "pricat";
    public static final String ARTICLES_PRICAT_EXPORT_TXT = ".txt";
    public static final String ARTICLES_PRICAT_EXPORT_HASH = ".hash";

    // Normal transaction files
    public static final String CSV_FILE_FORMAT = ".csv";
    public static final String HASH_FILE_FORMAT = ".hash";

    // AA bag/transaction files
    public static final String BATCH_FILE_FORMAT = ".batch";
    public static final String BATCH_HASH_FILE_FORMAT = "_batch.hash";
    public static final String SLS_FILE_FORMAT = ".sls";
    public static final String SLS_HASH_FILE_FORMAT = "_sls.hash";
    public static final String NLS_FILE_FORMAT = ".nls";
    public static final String NLS_HASH_FILE_FORMAT = "_nls.hash";
    public static final String READY_FILE_FORMAT = ".ready";
    public static final String READY_HASH_FILE_FORMAT = "_ready.hash";

    // Confirmed file
    public static final String CONFIRMED_FILE_FORMAT = ".confirmed";

    public static final String ERROR_FILE_FORMAT = ".error";
    public static final String LOG_FILE_FORMAT = ".log";
    public static final String FILE_TYPE_TRANSACTION = "TRANSACTION";
    public static final String FILE_TYPE_AA_BAG = "AA_BAG";
    public static final String FILE_TYPE_AA_TRANSACTION = "AA_TRANSACTION";
    private static final String FAILED_MOVE = "Failed to move file";

    public static final List<String> ALLOWED_BAG_TYPES = List.of("BB", "SB", "CB", "MB");

    public static Set<PosixFilePermission> fullPermissionsForFile() {
        Set<PosixFilePermission> fullPermission = new HashSet<PosixFilePermission>();
        fullPermission.add(PosixFilePermission.OWNER_EXECUTE);
        fullPermission.add(PosixFilePermission.OWNER_READ);
        fullPermission.add(PosixFilePermission.OWNER_WRITE);

        fullPermission.add(PosixFilePermission.GROUP_EXECUTE);
        fullPermission.add(PosixFilePermission.GROUP_READ);
        fullPermission.add(PosixFilePermission.GROUP_WRITE);

        fullPermission.add(PosixFilePermission.OTHERS_EXECUTE);
        fullPermission.add(PosixFilePermission.OTHERS_READ);
        fullPermission.add(PosixFilePermission.OTHERS_WRITE);

        return fullPermission;
    }

    /**
     * Move if exists.
     *
     * @param toDir the to dir
     * @param paths the paths
     */
    public static void moveIfExists(Path toDir, Path... paths) {
        for (Path path : paths) {
            if (Files.exists(path)) {
                try {
                    Files.move(path, toDir.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    Files.setPosixFilePermissions(toDir.resolve(path.getFileName()), fullPermissionsForFile());
                } catch (IOException e) {
                    LOGGER.error(FAILED_MOVE, e);
                } catch (UnsupportedOperationException e) {
                    // This is for windows it does not need posix permissions
                    LOGGER.debug("Posix permissions not supported, trying other way: " + toDir.resolve(path.getFileName()).toString(), e);
                }
            }
        }
    }

    /**
     * copy if exists.
     *
     * @param toDir the to dir
     * @param paths the paths
     */
    public static void copyIfExists(Path toDir, Path... paths) {
        for (Path path : paths) {
            if (Files.exists(path)) {
                try {
                    if (!Files.exists(toDir)) {
                        Files.createDirectories(toDir);
                    }
                    Files.copy(path, toDir.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    Files.setPosixFilePermissions(toDir.resolve(path.getFileName()), fullPermissionsForFile());
                } catch (IOException e) {
                    LOGGER.error(FAILED_MOVE, e);
                } catch (UnsupportedOperationException e) {
                    // This is for windows it does not need posix permissions
                    LOGGER.debug("Posix permissions not supported, trying other way: " + toDir.resolve(path.getFileName()).toString(), e);
                }
            }
        }
    }

    /**
     * Move and rename if exists.
     *
     * @param toDir    the to dir
     * @param path     the paths
     * @param renameTo rename to
     */
    public static void moveAndRenameIfExists(Path toDir, Path path, String renameTo) {
        if (Files.exists(path)) {
            try {
                Files.move(path, toDir.resolve(renameTo), StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(toDir.resolve(renameTo), fullPermissionsForFile());
            } catch (IOException e) {
                LOGGER.error(FAILED_MOVE, e);
            } catch (UnsupportedOperationException e) {
                // This is for windows it does not need posix permissions
                LOGGER.debug("Posix permissions not supported, trying other way: " + toDir.resolve(renameTo).toString(), e);
            }
        }
    }

    /**
     * Copy and rename if exists.
     *
     * @param toDir    the to dir
     * @param path     the paths
     * @param renameTo rename to
     */
    public static void copyAndRenameIfExists(Path toDir, Path path, String renameTo) {
        if (Files.exists(path)) {
            try {
                Files.copy(path, toDir.resolve(renameTo), StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(toDir.resolve(renameTo), fullPermissionsForFile());
            } catch (IOException e) {
                LOGGER.error(FAILED_MOVE, e);
            } catch (UnsupportedOperationException e) {
                // This is for windows it does not need posix permissions
                LOGGER.debug("Posix permissions not supported, trying other way: " + toDir.resolve(renameTo).toString(), e);
            }
        }
    }

    public static String getFilename(Path pathToFile) {
        String readyFileName = pathToFile.getFileName().toString();
        return readyFileName.substring(0, readyFileName.lastIndexOf("."));
    }

    public static List<ImportMessage> checkMissingImportTypes(Path file, boolean isAA) throws IOException {
        List<ImportMessage> failedChecks = new ArrayList<>();
        List<String> fileLines = Files.readAllLines(file);
        if (fileLines.stream().noneMatch(fileLine -> fileLine.contains("HDR"))) {
            failedChecks.add(new ImportMessage(0, "File has no HDR import type line"));
        }
        if (!isAA && fileLines.stream().noneMatch(fileLine -> fileLine.contains("POS"))) {
            failedChecks.add(new ImportMessage(0, "File has no POS import type line(s)"));
        }
        if (fileLines.stream().noneMatch(fileLine -> fileLine.contains("SUM"))) {
            failedChecks.add(new ImportMessage(0, "File has no SUM import type line"));
        }
        return failedChecks;
    }

    @Deprecated
    public static List<ImportMessage> checkFileHeader(String number, String version, Scanner scanner,
                                                      ImportType importType, int rejectDataOlderThanDays) {
        List<ImportMessage> failedChecks = new ArrayList<>();
        String versionNumber = scanner.next(); // message version
        if (ValidationUtils.versionIsNotValid(versionNumber, version)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Wrong version number was " + versionNumber + ", expected: " + version));
        }

        failedChecks.addAll(validateDate(scanner, importType, DATETIMEFORMATTER, rejectDataOlderThanDays));
        return failedChecks;
    }

    public static List<ImportMessage> checkFileContentHeader(String number, String version, FileHeader header,
                                                             ImportType importType, int rejectDataOlderThanDays) {
        List<ImportMessage> failedChecks = new ArrayList<>();
        String versionNumber = header.getVersion(); // message version
        if (ValidationUtils.versionIsNotValid(versionNumber, version)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Wrong version number was " + versionNumber + ", expected: " + version));
        }

        failedChecks.addAll(validateDateString(header.getDateTime(), importType, DATETIMEFORMATTER, rejectDataOlderThanDays));
        return failedChecks;
    }

    @Deprecated
    public static List<ImportMessage> validateDate(Scanner scanner, ImportType importType, DateTimeFormatter formatter,
                                                   int rejectDataOlderThanDays) {
        List<ImportMessage> failedChecks = new ArrayList<>();

        LocalDateTime dateTime = LocalDateTime.parse(scanner.next(), formatter); // message datetime
        if (dateTime.isAfter(LocalDateTime.now())) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Date is in future " + dateTime.format(READABLE_DATE_PATTERN) + ", expected date before: " + LocalDateTime.now().format(READABLE_DATE_PATTERN)));
        }
        if (dateTime.isBefore(LocalDateTime.now().minusDays(rejectDataOlderThanDays))) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Date is too far in the past " + dateTime.format(READABLE_DATE_PATTERN) + ", expected date after: " + LocalDateTime.now().minusDays(rejectDataOlderThanDays).format(READABLE_DATE_PATTERN)));
        }

        return failedChecks;
    }

    public static List<ImportMessage> validateDateString(String date, ImportType importType, DateTimeFormatter formatter,
                                                         int rejectDataOlderThanDays) {
        List<ImportMessage> failedChecks = new ArrayList<>();

        LocalDateTime dateTime = LocalDateTime.parse(date, formatter); // message datetime
        if (dateTime.isAfter(LocalDateTime.now())) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Date is in future " + dateTime.format(READABLE_DATE_PATTERN) + ", expected date before: " + LocalDateTime.now().format(READABLE_DATE_PATTERN)));
        }
        if (dateTime.isBefore(LocalDateTime.now().minusDays(rejectDataOlderThanDays))) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Date is too far in the past " + dateTime.format(READABLE_DATE_PATTERN) + ", expected date after: " + LocalDateTime.now().minusDays(rejectDataOlderThanDays).format(READABLE_DATE_PATTERN)));
        }

        return failedChecks;
    }

    @Deprecated
    public static List<ImportMessage> checkFileSum(String number, String version, Scanner scanner, ImportType importType, AtomicInteger posCount) {
        List<ImportMessage> failedChecks = new ArrayList<>();
        String totalAmount = scanner.next(); // total amount
        if (StringUtils.isEmpty(totalAmount)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Total amount field is empty"));
        } else {
            if (posCount.get() != Integer.parseInt(totalAmount)) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Total read amount is " + posCount.get() + ", does not equal total amount field value: " + Integer.parseInt(totalAmount)));
            }
        }
        return failedChecks;
    }

    public static List<ImportMessage> checkFileSumFooter(Company company, FileFooter fileFooter, ImportType importType, AtomicInteger posCount) {
        List<ImportMessage> failedChecks = new ArrayList<>();
        String totalAmount = fileFooter.getTotal(); // total amount
        if (StringUtils.isEmpty(totalAmount)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Total amount field is empty"));
        } else {
            if (posCount.get() != Integer.parseInt(totalAmount)) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Total read amount is " + posCount.get() + ", does not equal total amount field value: " + Integer.parseInt(totalAmount)));
            }
        }
        return failedChecks;
    }

    public static MailData createMail(String subject, String fileName, List<ImportMessage> failedChecks, List<String> fileImportFailedMailTo) {
        final MailData mailData = new MailData(subject).addTo(fileImportFailedMailTo).html(true);
        StringBuilder text = new StringBuilder("One or more import checks failed for the file(s) ").append(fileName).append("\n\n");
        failedChecks.forEach(failedCheck -> text.append(" - Line number: ").append(failedCheck.getLineNumber()).append(" ")
                .append(failedCheck.getMessage()).append("\n"));
        mailData.text(text.toString());
        mailData.emailType(BaseMessageType.SYSTEM);
        return mailData;
    }

    public static void readFileWithLineNumber(Path path, BiConsumer<Scanner, ImportType> function) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int lineNumber = reader.getLineNumber();
                try (Scanner scanner = new Scanner(line)) {
                    scanner.useDelimiter(";|(\r)?\n");
                    while (scanner.hasNext(HEADER_REGEX)) {
                        String type = scanner.next(HEADER_REGEX);
                        function.accept(scanner, new ImportType(lineNumber, type));
                        while (scanner.hasNext() && !scanner.hasNext(HEADER_REGEX)) {
                            scanner.next();
                        }
                    }
                }
            }
        }
    }

    public static String getLabelOrTransactionNumberFromFileName(Path pathToFile) {
        String fileName = pathToFile.getFileName().toString();
        return fileName.substring(fileName.lastIndexOf("-") + 1, fileName.lastIndexOf("."));
    }

    public static String getLabelOrTransactionNumberFromFileName(String fileName) {
        return fileName.substring(fileName.lastIndexOf("-") + 1, fileName.lastIndexOf("."));
    }

    /**
     * Gets customer number from label.
     *
     * @param fullLabel the full label
     * @return the customer number from label
     */
    public static String getCustomerNumberFromLabel(String fullLabel) {
        return fullLabel.substring(0, 7);
    }

    /**
     * Gets label number from label.
     *
     * @param fullLabel the full label
     * @return the label number from label
     */
    public static Integer getLabelNumberFromLabel(String fullLabel) {
        return Integer.valueOf(fullLabel.substring(7).trim());
    }
}
