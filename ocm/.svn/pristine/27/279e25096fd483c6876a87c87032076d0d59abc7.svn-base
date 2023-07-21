package com.tible.ocm.services.impl;

import com.google.common.base.Strings;
import com.tible.hawk.core.controllers.helpers.MailData;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.helper.AAFiles;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.dto.report.HLZHeader;
import com.tible.ocm.dto.report.ReportContent;
import com.tible.ocm.dto.report.body.BatchBody;
import com.tible.ocm.dto.report.body.SlsNlsBody;
import com.tible.ocm.models.CsvRecordType;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.MaterialTypeCode;
import com.tible.ocm.models.OcmVersion;
import com.tible.ocm.models.mongo.*;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.*;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.tible.ocm.models.MaterialTypeCode.getMaterialTypeByAACodeInt;
import static com.tible.ocm.services.log.LogKeyConstant.COMMUNICATION_KEY;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportedFileValidationHelper.createErrorFile;
import static com.tible.ocm.utils.ImportedFileValidationHelper.processWorkWithErrorFile;
import static com.tible.ocm.utils.OcmFileUtils.getAAFiles;
import static com.tible.ocm.utils.ValidationUtils.versionIsNotValid;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.math.NumberUtils.DOUBLE_ZERO;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class AAFilesServiceImpl implements AAFilesService {

    @Value("#{'${mail-to.file-import-failed}'.split(',')}")
    private List<String> fileImportFailedMailTo;

    @Value("${bag.range.big-max-weight}")
    private double bigMaxWeight;

    private final DirectoryService directoryService;
    private final BaseMailService mailService;
    private final TransactionService transactionService;
    private final ExistingBagService existingBagService;
    private final ExistingTransactionService existingTransactionService;
    private final LogExporterService<LogFileInfo> loggerExporterService;
    private final LabelOrderService labelOrderService;
    private final SrnArticleService srnArticleService;
    private final ImporterRuleService importerRuleService;
    private final CompanyService companyService;
    private final RejectedTransactionService rejectedTransactionService;
    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter HEADER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter BODY_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmssSS")
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
    private static final DateTimeFormatter BODY_DATE_TIME_FORMATTER_SECOND = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmssS")
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
    private static final DateTimeFormatter BODY_DATE_TIME_FORMATTER_THIRD = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmss")
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
    private static final DateTimeFormatter BODY_DATE_TIME_FORMATTER_FOURTH = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmssSSS")
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
    private static final DateTimeFormatter BATCH_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();

    @Override
    public void processAABagFiles(Company company, Path readyPath, boolean moveFailedToCompanyRejectedDirectory) {
        final Path accepted = directoryService.getBagsAcceptedPath();
        final Path rejected = directoryService.getBagsRejectedPath();
        final Path failed = directoryService.getBagsFailedPath();
        final Path backup = directoryService.getBagsBackupPath();
        final Path alreadyExists = directoryService.getBagsAlreadyExistsPath();
        processFiles(company, readyPath, moveFailedToCompanyRejectedDirectory, accepted, rejected, failed, backup, alreadyExists, false);
    }

    @Override
    public void processAATransactionFiles(Company company, Path readyPath, boolean moveFailedToCompanyRejectedDirectory) {
        final Path accepted = directoryService.getTransactionsAcceptedPath();
        final Path rejected = directoryService.getTransactionsRejectedPath();
        final Path failed = directoryService.getTransactionsFailedPath();
        final Path backup = directoryService.getTransactionsBackupPath();
        final Path alreadyExists = directoryService.getTransactionsAlreadyExistsPath();
        processFiles(company, readyPath, moveFailedToCompanyRejectedDirectory, accepted, rejected, failed, backup, alreadyExists, true);
    }

    @Override
    public void processAABackupOrFailedFiles(Company company, Path readyPath, boolean failedDir) {
        final Path companyTransPath = directoryService.getRoot().resolve(company.getIpAddress()).resolve(TRANS_DIRECTORY);

        String fileNameBase = getFilename(readyPath);
        final Path parent = readyPath.getParent();
        AAFiles aaFiles = getAAFiles(readyPath, parent, fileNameBase);

        if (failedDir) {
            moveIfExists(companyTransPath, aaFiles.getBatchPath());
            moveIfExists(companyTransPath, aaFiles.getBatchHashPath());
            moveIfExists(companyTransPath, aaFiles.getSlsPath());
            moveIfExists(companyTransPath, aaFiles.getSlsHashPath());
            moveIfExists(companyTransPath, aaFiles.getNlsPath());
            moveIfExists(companyTransPath, aaFiles.getNlsHashPath());
            moveIfExists(companyTransPath, aaFiles.getReadyPath());
            moveIfExists(companyTransPath, aaFiles.getReadyHashPath());
        } else {
            copyIfExists(companyTransPath, aaFiles.getBatchPath());
            copyIfExists(companyTransPath, aaFiles.getBatchHashPath());
            copyIfExists(companyTransPath, aaFiles.getSlsPath());
            copyIfExists(companyTransPath, aaFiles.getSlsHashPath());
            copyIfExists(companyTransPath, aaFiles.getNlsPath());
            copyIfExists(companyTransPath, aaFiles.getNlsHashPath());
            copyIfExists(companyTransPath, aaFiles.getReadyPath());
            copyIfExists(companyTransPath, aaFiles.getReadyHashPath());
        }
    }

    private void processFiles(Company company, Path readyPath, boolean moveFailedToCompanyRejectedDirectory, Path accepted,
                              Path rejected, Path failed, Path backup, Path alreadyExists, boolean isProcessingTransactions) {
        final Path rejectedCompany = rejected.resolve(company.getNumber());
        final Path failedCompany = failed.resolve(company.getIpAddress());
        final Path companyIpPath = directoryService.getRoot().resolve(company.getIpAddress());
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(accepted)) {
            log.error("Creating accepted directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(rejected)) {
            log.error("Creating rejected directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(failed)) {
            log.error("Creating failed directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(backup)) {
            log.error("Creating backup directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(alreadyExists)) {
            log.error("Creating alreadyExists directory failed");
        }

        String fileNameBase = getFilename(readyPath);
        final Path parent = readyPath.getParent();
        AAFiles aaFiles = getAAFiles(readyPath, parent, fileNameBase);

        LogFileInfo logFileInfo = LogFileInfo.builder()
                .fileName(fileNameBase)
                .isNeedExport(true)
                .path(isProcessingTransactions ? directoryService.getTransactionLogPath() : directoryService.getBagLogPath()).build();
        Map<String, Object> contentMap = new HashMap<>(Map.of(COMMUNICATION_KEY, company.getCommunication()));
        OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);
        OcmFileUtils.checkOrCreateDirWithFullPermissions(failedCompany);

        List<SrnArticle> articles = srnArticleService.getAll();
        List<ImportMessage> failedChecks = new ArrayList<>();
        try {
            if (!Files.exists(aaFiles.getBatchPath()) || !Files.exists(aaFiles.getSlsPath()) || !Files.exists(aaFiles.getNlsPath())) {
                String message = String.format(
                        "Moving %s AA files to rejected folder, because not all required files (.batch/.sls/.nls) are present", fileNameBase);
                log.warn(message);
                failedChecks.add(new ImportMessage(0, message));
                logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                        rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                return;
            }

            if (!Files.exists(aaFiles.getReadyHashPath()) || !Files.exists(aaFiles.getBatchHashPath()) ||
                    !Files.exists(aaFiles.getSlsHashPath()) || !Files.exists(aaFiles.getNlsHashPath())) {
                String message = String.format(
                        "Moving %s AA files to rejected folder, because not all required hash files (_ready.hash/_batch.hash/_sls.hash/_nls.hash) are present", fileNameBase);
                log.warn(message);
                failedChecks.add(new ImportMessage(0, message));
                logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                        rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                return;
            }

            // Do checks of hash files
            //failedChecks.addAll(validateHashFile(aaFiles.getReadyPath(), aaFiles.getReadyHashPath())); //Ready file is empty, so no hash possible
            failedChecks.addAll(validateHashFile(aaFiles.getBatchPath(), aaFiles.getBatchHashPath()));
            failedChecks.addAll(validateHashFile(aaFiles.getSlsPath(), aaFiles.getSlsHashPath()));
            failedChecks.addAll(validateHashFile(aaFiles.getNlsPath(), aaFiles.getNlsHashPath()));
            if (!failedChecks.isEmpty()) {
                logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                        rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                return;
            }

            String labelOrTransactionNumber = getLabelOrTransactionNumberFromFileName(readyPath);
            if (Files.exists(accepted.resolve(buildFileName(labelOrTransactionNumber, company.getNumber(), CSV_FILE_FORMAT)))) {
                String message = String.format(
                        "Moving %s AA files to already exists folder, because file is already processed and accepted", fileNameBase);
                log.warn(message);

                if (company.isNotifyAboutDoubleTransactions()) {
                    failedChecks.add(new ImportMessage(0, message));
                    logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                            rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                    return;
                }

                logMessageAlreadyExists(failedChecks, contentMap, logFileInfo, aaFiles, alreadyExists, company);
                return;
            }

            // Check if transaction is unique
            if (transactionRepository.existsByTransactionNumber(labelOrTransactionNumber)) {
                String message = String.format("Moving %s AA files to already exists folder, because transaction file is already processed", fileNameBase);
                log.warn(message);

                if (company.isNotifyAboutDoubleTransactions()) {
                    failedChecks.add(new ImportMessage(0, message));
                    logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                            rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                    return;
                }

                logMessageAlreadyExists(failedChecks, contentMap, logFileInfo, aaFiles, alreadyExists, company);
                return;
            }

            if (isProcessingTransactions) {
                String machineId = labelOrTransactionNumber.substring(1, 5);
                if (company.getSerialNumbers().stream().noneMatch(machineId::equals)) {
                    String message = String.format("%s file the company %s (%s) from which this file is received from does not have the following AA machine id %s, machine id is retrieved from the filename",
                            fileNameBase, company.getName(), company.getNumber(), machineId);
                    log.warn(message);
                    failedChecks.add(new ImportMessage(0, message));
                    logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                            rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                    return;
                }

                if (existingTransactionService.lazyCheckIsTransactionAlreadyExists(labelOrTransactionNumber, company.getRvmOwnerNumber())) {
                    String message = String.format(
                            "Transaction is already processed at SNL for %s", fileNameBase);
                    log.warn(message);

                    if (company.isNotifyAboutDoubleTransactions()) {
                        failedChecks.add(new ImportMessage(0, message));
                        logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                                rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                        return;
                    }

                    logMessageAlreadyExists(failedChecks, contentMap, logFileInfo, aaFiles, alreadyExists, company);
                    return;
                }
            } else {
                String customerNumberFromLabel = getCustomerNumberFromLabel(labelOrTransactionNumber);
                if (!companyService.existsByNumber(customerNumberFromLabel)) {
                    String message = String.format(
                            "Customer %s of the label %s does not exist for %s", customerNumberFromLabel, labelOrTransactionNumber, fileNameBase);
                    log.warn(message);
                    failedChecks.add(new ImportMessage(0, message));
                    logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                            rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                    return;
                }

                Integer numberFromLabel = getLabelNumberFromLabel(labelOrTransactionNumber);
                String labelOrTransactionNumberWithZeros = customerNumberFromLabel + Strings.padStart(String.valueOf(numberFromLabel), 9, '0');
                if (existingBagService.lazyCheckIsBagAlreadyExists(labelOrTransactionNumber) || existingBagService.lazyCheckIsBagAlreadyExists(labelOrTransactionNumberWithZeros)) {
                    String message = String.format(
                            "Label is already processed at SNL for %s", fileNameBase);
                    log.warn(message);

                    if (company.isNotifyAboutDoubleTransactions()) {
                        failedChecks.add(new ImportMessage(0, message));
                        logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                                rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                        return;
                    }

                    logMessageAlreadyExists(failedChecks, contentMap, logFileInfo, aaFiles, alreadyExists, company);
                    return;
                }

                // Check transaction bag label already exists at SRN existingTransactionService
                if (existingTransactionService.existsByCombinedCustomerNumberLabel(labelOrTransactionNumber)) {
                    String message = String.format(
                            "Label is already processed at SNL for %s", fileNameBase);
                    log.warn(message);

                    if (company.isNotifyAboutDoubleTransactions()) {
                        failedChecks.add(new ImportMessage(0, message));
                        logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                                rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                        return;
                    }

                    logMessageAlreadyExists(failedChecks, contentMap, logFileInfo, aaFiles, alreadyExists, company);
                    return;
                }

                // Check if transaction bag label is unique
                if (transactionRepository.existsByLabelNumber(labelOrTransactionNumber)) {
                    String message = String.format("Transaction with label number %s already exist.", labelOrTransactionNumber);
                    log.warn(message);

                    if (company.isNotifyAboutDoubleTransactions()) {
                        failedChecks.add(new ImportMessage(0, message));
                        logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                                rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                        return;
                    }

                    logMessageAlreadyExists(failedChecks, contentMap, logFileInfo, aaFiles, alreadyExists, company);
                    return;
                }

                Company customerByNumber = companyService.findByNumber(customerNumberFromLabel);
                if (customerByNumber != null) {
                    if (customerByNumber.getRvmOwnerNumber() != null) {
                        if (!labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                customerNumberFromLabel, company.getRvmOwnerNumber(), Long.valueOf(numberFromLabel))) {
                            failedChecks.add(new ImportMessage(0, "Label number " + labelOrTransactionNumber + " is using the wrong label number for the wrong customer number"));
                        }
                    } else {
                        if (!labelOrderService.existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                customerNumberFromLabel, Long.valueOf(numberFromLabel))) {
                            failedChecks.add(new ImportMessage(0, "Label number " + labelOrTransactionNumber + " is using the wrong label number for the wrong customer number"));
                        }
                    }
                } else {
                    if (!labelOrderService.existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                            customerNumberFromLabel, Long.valueOf(numberFromLabel))) {
                        failedChecks.add(new ImportMessage(0, "Label number " + labelOrTransactionNumber + " is using the wrong label number for the wrong customer number"));
                    }
                }
            }

            List<ImportMessage> missingImportTypesMessages = validateMissingImportTypes(aaFiles.getBatchPath(), aaFiles.getSlsPath(), aaFiles.getNlsPath());
            failedChecks.addAll(missingImportTypesMessages);

            Pair<List<ImportMessage>, ReportContent<BatchBody>> batchBodyReportContent = readFile(aaFiles.getBatchPath(), BatchBody::body);
            Pair<List<ImportMessage>, ReportContent<SlsNlsBody>> slsBodyReportContent = readFile(aaFiles.getSlsPath(), SlsNlsBody::body);
            Pair<List<ImportMessage>, ReportContent<SlsNlsBody>> nlsBodyReportContent = readFile(aaFiles.getNlsPath(), SlsNlsBody::body);

            failedChecks.addAll(batchBodyReportContent.getLeft());
            failedChecks.addAll(slsBodyReportContent.getLeft());
            failedChecks.addAll(nlsBodyReportContent.getLeft());

            failedChecks.addAll(validateContentStructure(batchBodyReportContent, this::checkBatchBody, company));
            failedChecks.addAll(validateContentStructure(slsBodyReportContent, this::checkSlsBody, company));
            failedChecks.addAll(validateContentStructure(nlsBodyReportContent, this::checkNlsBody, company));

            failedChecks.addAll(validateContentFiles(labelOrTransactionNumber, company, slsBodyReportContent, nlsBodyReportContent,
                    batchBodyReportContent, isProcessingTransactions, articles));

            if (!failedChecks.isEmpty()) {
                MailData mailData = createMail("OCM one or more of the import checks for files of " + fileNameBase + " failed",
                        fileNameBase, failedChecks, fileImportFailedMailTo);
                // mailService.sendMail(mailData, null); // Turned off for now

                log.warn("Moving {} AA files to rejected folder, because one or more import checks failed", fileNameBase);

                logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                        rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
                return;
            }

            String newReadyFileName = buildFileName(fileNameBase, company.getNumber(), READY_FILE_FORMAT);
            String newReadyHashFileName = buildFileName(fileNameBase, company.getNumber(), READY_HASH_FILE_FORMAT);
            String newBatchFileName = buildFileName(fileNameBase, company.getNumber(), BATCH_FILE_FORMAT);
            String newBatchHashFileName = buildFileName(fileNameBase, company.getNumber(), BATCH_HASH_FILE_FORMAT);
            String newSlsFileName = buildFileName(fileNameBase, company.getNumber(), SLS_FILE_FORMAT);
            String newSlsHashFileName = buildFileName(fileNameBase, company.getNumber(), SLS_HASH_FILE_FORMAT);
            String newNlsFileName = buildFileName(fileNameBase, company.getNumber(), NLS_FILE_FORMAT);
            String newNlsHashFileName = buildFileName(fileNameBase, company.getNumber(), NLS_HASH_FILE_FORMAT);

            processContentFiles(labelOrTransactionNumber, company, slsBodyReportContent, nlsBodyReportContent, batchBodyReportContent, isProcessingTransactions);

            if (OcmFileUtils.checkOrCreateDirWithFullPermissions(backup)) {
                Path backupIpPath = backup.resolve(company.getIpAddress());
                if (OcmFileUtils.checkOrCreateDirWithFullPermissions(backupIpPath)) {
                    moveIfExists(backupIpPath, aaFiles.getReadyPath());
                    moveIfExists(backupIpPath, aaFiles.getReadyHashPath());
                    moveIfExists(backupIpPath, aaFiles.getBatchPath());
                    moveIfExists(backupIpPath, aaFiles.getBatchHashPath());
                    moveIfExists(backupIpPath, aaFiles.getSlsPath());
                    moveIfExists(backupIpPath, aaFiles.getSlsHashPath());
                    moveIfExists(backupIpPath, aaFiles.getNlsPath());
                    moveIfExists(backupIpPath, aaFiles.getNlsHashPath());
                }
            }

            //contentMap.put(DETAILS_KEY, "AA files were handled successfully");
            //loggerExporterService.logToFile(logFileInfo, contentMap);
            loggerExporterService.exportWithContentMap(aaFiles.getReadyPath(), contentMap, failedChecks, logFileInfo, company, false, IMPORT_STATUS_ACCEPTED);
        } catch (NoSuchElementException e) {
            String message = String.format(
                    "Moving %s AA files to rejected folder, because one of the files is missing a field",
                    fileNameBase);
            log.warn(message, e);
            failedChecks.add(new ImportMessage(0, message));
            logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                    rejectedCompany, moveFailedToCompanyRejectedDirectory, company);
        } catch (Exception e) {
            String message = String.format(
                    "Moving %s AA files to failed folder, because of the next error: %s",
                    fileNameBase, e.getMessage());
            log.warn(message, e);
            failedChecks.add(new ImportMessage(0, message));
            createErrorFile(fileNameBase, aaFiles.getErrorFile(), failedChecks);

            loggerExporterService.exportWithContentMap(aaFiles.getReadyPath(), contentMap, failedChecks, logFileInfo, company, false, IMPORT_STATUS_FAILED);

            /*logMessageAndErrorFile(failedChecks, contentMap, logFileInfo, aaFiles, companyIpPath,
                    rejectedCompany, moveFailedToCompanyRejectedDirectory, company);*/
            moveIfExists(failedCompany, aaFiles.getReadyPath(), aaFiles.getReadyHashPath(), aaFiles.getBatchPath(), aaFiles.getBatchHashPath(),
                    aaFiles.getSlsPath(), aaFiles.getSlsHashPath(), aaFiles.getNlsPath(), aaFiles.getNlsHashPath(), aaFiles.getErrorFile());
        }
    }

    private void logMessageAndErrorFile(List<ImportMessage> failedChecks, Map<String, Object> contentMap, LogFileInfo logFileInfo,
                                        AAFiles aaFiles, Path companyIpPath, Path rejectedCompany, boolean moveFailedToCompanyRejectedDirectory,
                                        Company company) {
        String fileNameBase = getFilename(aaFiles.getReadyPath());
        createErrorFile(fileNameBase, aaFiles.getErrorFile(), failedChecks);
        saveRejectedTransaction(fileNameBase, company.getNumber());

        //contentMap.put(DETAILS_KEY, getErrorDetailsMessage(fileNameBase));
        //contentMap.put(IMPORT_MESSAGES_KEY, failedChecks);
        //loggerExporterService.logToFile(logFileInfo, contentMap);
        loggerExporterService.exportWithContentMap(aaFiles.getReadyPath(), contentMap, failedChecks, logFileInfo, company, false, IMPORT_STATUS_REJECTED);

        processWorkWithErrorFile(companyIpPath, TRANS_DIRECTORY, moveFailedToCompanyRejectedDirectory,
                rejectedCompany, aaFiles.getReadyPath(), aaFiles.getReadyHashPath(), aaFiles.getBatchPath(), aaFiles.getBatchHashPath(),
                aaFiles.getSlsPath(), aaFiles.getSlsHashPath(), aaFiles.getNlsPath(), aaFiles.getNlsHashPath(), aaFiles.getErrorFile());
    }

    private void saveRejectedTransaction(String fileNameBase, String companyNumber) {
        RejectedTransaction rejectedTransaction = RejectedTransaction
                .builder()
                .baseFileName(fileNameBase)
                .createdAt(LocalDateTime.now())
                .type(RejectedTransaction.TransactionType.BAG)
                .companyNumber(companyNumber)
                .isExternal(Boolean.FALSE)
                .needToBeDeleted(Boolean.FALSE)
                .build();

        rejectedTransactionService.save(rejectedTransaction);
    }

    private void logMessageAlreadyExists(List<ImportMessage> failedChecks, Map<String, Object> contentMap, LogFileInfo logFileInfo,
                                         AAFiles aaFiles, Path alreadyExistsPath, Company company) {
        loggerExporterService.exportWithContentMap(aaFiles.getReadyPath(), contentMap, failedChecks, logFileInfo, company, true, IMPORT_STATUS_ALREADY_EXISTS);

        moveIfExists(alreadyExistsPath, aaFiles.getReadyPath(), aaFiles.getReadyHashPath(), aaFiles.getBatchPath(), aaFiles.getBatchHashPath(),
                aaFiles.getSlsPath(), aaFiles.getSlsHashPath(), aaFiles.getNlsPath(), aaFiles.getNlsHashPath());
    }

    private List<ImportMessage> validateContentFiles(String labelOrTransactionNumber, Company company,
                                                     Pair<List<ImportMessage>, ReportContent<SlsNlsBody>> slsReport,
                                                     Pair<List<ImportMessage>, ReportContent<SlsNlsBody>> nlsReport,
                                                     Pair<List<ImportMessage>, ReportContent<BatchBody>> batchReport,
                                                     boolean isProcessingTransactions, List<SrnArticle> articles) {
        List<ImportMessage> messages = new ArrayList<>();

        HLZHeader slsHeader = slsReport.getRight().getHeader();
        HLZHeader nlsHeader = nlsReport.getRight().getHeader();
        HLZHeader batchHeader = batchReport.getRight().getHeader();

        List<SlsNlsBody> slsBodies = slsReport.getRight().getBody();
        List<SlsNlsBody> nlsBodies = nlsReport.getRight().getBody();
        List<BatchBody> batchBodies = batchReport.getRight().getBody();
        if (batchBodies.size() != 1) {
            throw new IllegalArgumentException("Batch body can contain only one POS line");
        }
        BatchBody batchBody = batchBodies.get(0);

        boolean isFortRunningNumberNotEqual = new HashSet<>(List.of(slsHeader.getFortRunningNumber(),
                nlsHeader.getFortRunningNumber(),
                batchHeader.getFortRunningNumber()))
                .size() != 1;

        if (isFortRunningNumberNotEqual) {
            messages.add(new ImportMessage(0, "Fort running number not equal between sls/nls/batch files"));
        }

        int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
        if (batchBody.getBatchTimeStart().length() == 17) {
            messages.addAll(validateDateString(batchBody.getBatchTimeStart(), new ImportType(2, "POS"), BODY_DATE_TIME_FORMATTER_FOURTH, dataExpirationPeriodInDays));
        } else if (batchBody.getBatchTimeStart().length() == 16) {
            messages.addAll(validateDateString(batchBody.getBatchTimeStart(), new ImportType(2, "POS"), BODY_DATE_TIME_FORMATTER, dataExpirationPeriodInDays));
        } else if (batchBody.getBatchTimeStart().length() == 15) {
            messages.addAll(validateDateString(batchBody.getBatchTimeStart(), new ImportType(2, "POS"), BODY_DATE_TIME_FORMATTER_SECOND, dataExpirationPeriodInDays));
        } else {
            messages.addAll(validateDateString(batchBody.getBatchTimeStart(), new ImportType(2, "POS"), BODY_DATE_TIME_FORMATTER_THIRD, dataExpirationPeriodInDays));
        }

        String machineId = batchBody.getKeyId();
        if (company.getSerialNumbers().stream().noneMatch(machineId::equals)) {
            messages.add(new ImportMessage(1,
                    String.format("%s file the company %s (%s) from which this file is received from does not have the following AA machine id %s",
                            batchReport.getRight().getFileName(), company.getName(), company.getNumber(), machineId)));
        }

        if (isProcessingTransactions) {
            String machineIdFromFileName = labelOrTransactionNumber.substring(1, 5);
            if (!machineId.equals(machineIdFromFileName)) {
                messages.add(new ImportMessage(1,
                        String.format("%s file the machine id %s from the filename does not equal batch file machine id %s",
                                batchReport.getRight().getFileName(), machineIdFromFileName, machineId)));
            }
        }

        validateEqualFieldsWithBatch(messages, slsBodies, SlsNlsBody::getBatchId, batchBody::getBatchId, "Batch ID not equal between sls/nls/batch files");
        validateEqualFieldsWithBatch(messages, nlsBodies, SlsNlsBody::getBatchId, batchBody::getBatchId, "Batch ID not equal between sls/nls/batch files");

        int batchSls = Integer.parseInt(batchBody.getNumberOfRefundable());
        if (batchSls != slsBodies.size()) {
            messages.add(new ImportMessage(1,
                    String.format("%s file the refundable amount of articles %s in sls does not equal batch file refundable amount %s",
                            slsReport.getRight().getFileName(), slsBodies.size(), batchSls)));
        }

        int batchNls = Integer.parseInt(batchBody.getNumberOfNonRefundable()) + Integer.parseInt(batchBody.getEanNotReadable()) + Integer.parseInt(batchBody.getUnknown());
        if (batchNls != nlsBodies.size()) {
            messages.add(new ImportMessage(1,
                    String.format("%s file the nonrefundable plus the not readable and unknown amount of articles %s in nls does not equal batch file nonrefundable plus the not readable and unknown amount %s",
                            nlsReport.getRight().getFileName(), nlsBodies.size(), batchNls)));
        }

        int batchTotal = Integer.parseInt(batchBody.getNumberInBatch());
        int slsAndNlsAmount = slsBodies.size() + nlsBodies.size();
        if (batchTotal != slsAndNlsAmount) {
            messages.add(new ImportMessage(1,
                    String.format("%s file the total amount of articles %s in sls and nls does not equal batch file amount in batch %s",
                            batchReport.getRight().getFileName(), slsAndNlsAmount, batchTotal)));
        }

        List<ImporterRule> importerRules = importerRuleService.getAllByRvmOwnerAndRvmSerial(company.getRvmOwnerNumber(), company.getSerialNumbers());
        double weightCheck = DOUBLE_ZERO;
        for (int index = 0; index < slsBodies.size(); index++) {
            SlsNlsBody body = slsBodies.get(index);
            int lineNumber = index + 2; // HDR is line number 1 and POS starts on line number 2.

            if (isProcessingTransactions) {
                String machineIdSls = body.getKeyId();
                String machineIdFromFileName = getLabelOrTransactionNumberFromFileName(slsReport.getRight().getFileName()).substring(1, 5);
                if (!StringUtils.isEmpty(machineIdSls) && !machineIdSls.equals(machineIdFromFileName)) {
                    messages.add(new ImportMessage(lineNumber,
                            String.format("%s file the machine id %s from the filename does not equal sls file machine id %s",
                                    slsReport.getRight().getFileName(), machineIdFromFileName, machineIdSls)));
                }
            }

            LocalDateTime checkDateTime;
            if (batchBody.getBatchTimeStart().length() == 17) {
                checkDateTime = LocalDateTime.parse(batchBody.getBatchTimeStart(), BODY_DATE_TIME_FORMATTER_FOURTH);
            } else if (batchBody.getBatchTimeStart().length() == 16) {
                checkDateTime = LocalDateTime.parse(batchBody.getBatchTimeStart(), BODY_DATE_TIME_FORMATTER);
            } else if (batchBody.getBatchTimeStart().length() == 15) {
                checkDateTime = LocalDateTime.parse(batchBody.getBatchTimeStart(), BODY_DATE_TIME_FORMATTER_SECOND);
            } else {
                checkDateTime = LocalDateTime.parse(batchBody.getBatchTimeStart(), BODY_DATE_TIME_FORMATTER_THIRD);
            }

            String ean;
            String eanText;
            Optional<ImporterRule> importerRuleOptional = importerRules
                    .stream()
                    .filter(importerRule -> importerRule.getFromEan().equals(body.getArticleNumber()))
                    .findFirst();
            if (importerRuleOptional.isPresent()) {
                ean = importerRuleOptional.get().getToEan();
                eanText = body.getArticleNumber() + " (" + importerRuleOptional.get().getToEan() + ")";
            } else {
                ean = body.getArticleNumber(); // article number
                eanText = body.getArticleNumber();
            }

            if (StringUtils.isEmpty(ean)) {
                messages.add(new ImportMessage(lineNumber,
                        String.format("%s file EAN field is empty", slsReport.getRight().getFileName())));
            } else if (articles.stream().noneMatch(article -> article.getNumber().equals(ean))
                    && importerRules.stream().noneMatch(importerRule -> importerRule.getFromEan().equals(ean))) {
                messages.add(new ImportMessage(lineNumber,
                        String.format("%s file the following ean %s does not exist", slsReport.getRight().getFileName(), eanText)));
            } else if (articles.stream().anyMatch(article -> article.getNumber().equals(ean) && article.getFirstArticleActivationDate() == null &&
                    article.getActivationDate() != null && article.getActivationDate().isAfter(checkDateTime))) {
                messages.add(new ImportMessage(lineNumber,
                        String.format("%s file the activation date is in the future for article with number %s", slsReport.getRight().getFileName(), eanText)));
            } else if (articles.stream().anyMatch(article -> article.getNumber().equals(ean) && article.getFirstArticleActivationDate() != null &&
                    article.getActivationDate() != null && article.getFirstArticleActivationDate().isBefore(article.getActivationDate()) &&
                    article.getFirstArticleActivationDate().isAfter(checkDateTime))) {
                messages.add(new ImportMessage(lineNumber,
                        String.format("%s file the activation date is in the future for article with number %s", slsReport.getRight().getFileName(), eanText)));
            } else {
                weightCheck += ofNullable(srnArticleService.findByArticleNumber(ean))
                        .map(this::countWeight)
                        .orElse(DOUBLE_ZERO);
            }
        }

        weightCheck = convertGramsToKilograms(weightCheck);

        //bigMaxWeight is used, because bag type in ocm is decided based on a bagtype to be added around september. If no value we see it as BB.
        /*if (!isProcessingTransactions && weightCheck > getMaxAllowedWeight(bigMaxWeight)) {
            messages.add(new ImportMessage(slsBodies.size(),
                    String.format("This does not seem to be a BB contents as the amount of 2%f kg is far more then the expected weight of a Big Bag. " +
                            "Please contact SNL in case you disagree.", convertGramsToKilograms(weightCheck))));
        }*/ // Turn it off for now until issue is better described https://jira.tible.com/browse/SRNPACK2-1476

        for (int index = 0; index < nlsBodies.size(); index++) {
            SlsNlsBody body = nlsBodies.get(index);
            int lineNumber = index + 2; // HDR is line number 1 and POS starts on line number 2.

            if (isProcessingTransactions) {
                String machineIdNls = body.getKeyId();
                String machineIdFromFileName = getLabelOrTransactionNumberFromFileName(nlsReport.getRight().getFileName()).substring(1, 5);
                if (!StringUtils.isEmpty(machineIdNls) && !machineIdNls.equals(machineIdFromFileName)) {
                    messages.add(new ImportMessage(lineNumber,
                            String.format("%s file the machine id %s from the filename does not equal nls file machine id %s",
                                    slsReport.getRight().getFileName(), machineIdFromFileName, machineIdNls)));
                }
            }
        }

        return messages;
    }

    private void processContentFiles(String labelOrTransactionNumber, Company company, Pair<List<ImportMessage>,
            ReportContent<SlsNlsBody>> slsReport, Pair<List<ImportMessage>, ReportContent<SlsNlsBody>> nlsReport,
                                     Pair<List<ImportMessage>, ReportContent<BatchBody>> batchReport, boolean isProcessingTransactions) {
        List<SlsNlsBody> slsBodies = slsReport.getRight().getBody();
        List<SlsNlsBody> nlsBodies = nlsReport.getRight().getBody();
        HLZHeader batchHeader = batchReport.getRight().getHeader();
        BatchBody batchBody = batchReport.getRight().getBody().get(0);

        Transaction transaction = new Transaction();
         if (ImportedFileValidationHelper.version17Check(company.getVersion())) {
            transaction.setVersion(batchHeader.getMessageVersionNumber());
        } else {
            transaction.setVersion(OcmVersion.VERSION_15.title);
        }
        String batchTimeStart = batchBody.getBatchTimeStart();
        if (batchTimeStart.length() == 17) {
            transaction.setDateTime(LocalDateTime.parse(batchTimeStart, BODY_DATE_TIME_FORMATTER_FOURTH));
        } else if (batchTimeStart.length() == 16) {
            transaction.setDateTime(LocalDateTime.parse(batchTimeStart, BODY_DATE_TIME_FORMATTER));
        } else if (batchTimeStart.length() == 15) {
            transaction.setDateTime(LocalDateTime.parse(batchTimeStart, BODY_DATE_TIME_FORMATTER_SECOND));
        } else {
            transaction.setDateTime(LocalDateTime.parse(batchTimeStart, BODY_DATE_TIME_FORMATTER_THIRD));
        }
        transaction.setStoreId(company.getStoreId());
        transaction.setSerialNumber(batchBody.getKeyId());
        if (isProcessingTransactions) {
            transaction.setTransactionNumber(labelOrTransactionNumber);
        } else {
            transaction.setTransactionNumber(batchBody.getBatchId());
        }
        transaction.setTotal(Integer.parseInt(batchBody.getNumberInBatch()));
        transaction.setRefundable(Integer.parseInt(batchBody.getNumberOfRefundable()));
        transaction.setCollected(Integer.parseInt(batchBody.getNumberInBatch()));
        if (isProcessingTransactions) {
            transaction.setType(FILE_TYPE_AA_TRANSACTION);
        } else {
            transaction.setType(FILE_TYPE_AA_BAG);
        }
        transaction.setReceivedDate(LocalDateTime.now());

        List<TransactionArticle> transactionArticles = new ArrayList<>();
        slsBodies.forEach(slsBody -> transactionArticles.add(processSlsNlsContent(slsBody, true)));
        nlsBodies.forEach(nlsBody -> transactionArticles.add(processSlsNlsContent(nlsBody, false)));

        transactionService.saveTransactionAndArticlesByCompany(transaction, transactionArticles, company);
    }

    private TransactionArticle processSlsNlsContent(SlsNlsBody slsNlsBody, boolean processingSls) {
        TransactionArticle transactionArticle = new TransactionArticle();
        transactionArticle.setArticleNumber(slsNlsBody.getArticleNumber());
        // transactionArticle.setScannedWeight();
        if (isParsable(slsNlsBody.getTypeOfMaterial())) {
            Optional<MaterialTypeCode> materialTypeCode = getMaterialTypeByAACodeInt(Integer.parseInt(slsNlsBody.getTypeOfMaterial()));
            if (materialTypeCode.isPresent()) {
                transactionArticle.setMaterial(materialTypeCode.get().getCodeInt());
            } else {
                transactionArticle.setMaterial(0);
            }
        } else {
            transactionArticle.setMaterial(0);
        }

        if (processingSls) {
            transactionArticle.setRefund(1);
        } else {
            transactionArticle.setRefund(0);
        }
        transactionArticle.setCollected(1);

        return transactionArticle;
    }

    private void validateEqualFieldsWithBatch(List<ImportMessage> messages,
                                              List<SlsNlsBody> slsBodies,
                                              Function<SlsNlsBody, String> extractorField,
                                              Supplier<String> extractorBatchField,
                                              String message) {
        for (int index = 0; index < slsBodies.size(); index++) {
            SlsNlsBody body = slsBodies.get(index);
            String extractedField = extractorField.apply(body);
            String extractedBatch = extractorBatchField.get();
            if (!extractedField.equals(extractedBatch)) {
                messages.add(new ImportMessage(index, message));
            }
        }
    }

    private String buildFileName(String fileNameBase, String companyNumber, String format) {
        return String.format("%s-%s%s", fileNameBase, companyNumber, format);
    }

    private <T> Pair<List<ImportMessage>, ReportContent<T>> readFile(Path file, Function<Pair<Scanner, ImportType>, T> buildBodyFunction) throws IOException {
        List<ImportMessage> failedChecks = new ArrayList<>();
        ReportContent<T> content = new ReportContent<>();
        content.setFileName(file.getFileName().toString());
        ArrayList<T> body = new ArrayList<>();
        readFileWithLineNumber(file, (scanner, importType) -> {
            switch (CsvRecordType.valueOf(importType.getType())) {
                case HDR:
                    content.setHeader(HLZHeader.header(scanner, importType));
                    if (scanner.hasNext()) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(),
                                String.format("%s file HDR amount of fields is not according to version: 2019-11-2106", file.getFileName().toString())));
                    }
                    break;
                case POS:
                    body.add(buildBodyFunction.apply(Pair.of(scanner, importType)));
                    if (scanner.hasNext()) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(),
                                String.format("%s file POS amount of fields is not according to version: 2019-11-2106", file.getFileName().toString())));
                    }
                    break;
                case SUM:
                    break;
            }
        });
        content.setBody(body);
        return Pair.of(failedChecks, content);
    }

    private <T> List<ImportMessage> validateContentStructure(Pair<List<ImportMessage>, ReportContent<T>> reportContent, BiFunction<Pair<String, T>, Integer, List<ImportMessage>> bodyFunction, Company company) {
        ReportContent<T> reportBody = reportContent.getRight();
        List<ImportMessage> importMessages = checkHeader(reportBody.getFileName(), reportBody.getHeader(), new ImportType(1, "HDR"), company);
        List<ImportMessage> messages = new ArrayList<>(importMessages);
        List<T> body = reportContent.getRight().getBody();
        IntStream.range(0, body.size())
                .mapToObj(index -> bodyFunction.apply(Pair.of(reportContent.getRight().getFileName(), body.get(index)), index))
                .flatMap(Collection::stream)
                .forEach(messages::add);
        return messages;
    }

    private List<ImportMessage> checkSlsBody(Pair<String, SlsNlsBody> fileNameAndBody, int line) {
        return checkBody(fileNameAndBody, true);
    }

    private List<ImportMessage> checkNlsBody(Pair<String, SlsNlsBody> fileNameAndBody, int line) {
        return checkBody(fileNameAndBody, false);
    }

    private List<ImportMessage> checkHeader(String fileName, HLZHeader header, ImportType importType, Company company) {
        List<ImportMessage> failedChecks = new ArrayList<>();

        ofNullable(validateEmptyField(fileName, header.getFortRunningNumber(), importType, "Fort-running number"))
                .ifPresent(failedChecks::add);

        ofNullable(validateDate(fileName, header.getDateOfCreation(), importType, HEADER_DATE_FORMATTER))
                .ifPresent(failedChecks::add);
        ofNullable(validateFieldWithValue(fileName, header.getConstant(), importType, "9", "HDR code"))
                .ifPresent(failedChecks::add);

        String glnFirstDistributor = header.getGlnFirstDistributer();
        if (StringUtils.isEmpty(glnFirstDistributor) || glnFirstDistributor.length() != 13) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("%s file GLN First distributor is empty", fileName)));
        }

        String glnServiceProvider = header.getGlnServiceProvider();
        if (StringUtils.isEmpty(glnServiceProvider) || glnServiceProvider.length() != 13) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("%s file GLN Service provider is empty", fileName)));
        }

        if (ImportedFileValidationHelper.version17Check(company.getVersion())) {
            String messageVersion = header.getMessageVersionNumber();
            /*Set<String> ocmVersions = EnumSet.allOf(OcmVersion.class)
                    .stream()
                    .map(ocmVersion -> ocmVersion.title)
                    .collect(Collectors.toSet());*/
            if (versionIsNotValid(messageVersion, company.getVersion())) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("%s file message version " +
                        "number is not a valid OCM version", fileName)));
            }
        }
        return failedChecks;
    }

    private List<ImportMessage> checkBody(Pair<String, SlsNlsBody> fileNameAndBody, boolean isSlsFile) {
        List<ImportMessage> failedChecks = new ArrayList<>();

        SlsNlsBody body = fileNameAndBody.getRight();
        String fileName = fileNameAndBody.getLeft();
        ImportType importType = body.getImportType();
        String glnManufacturer = body.getGlnManufacturer();
        if (StringUtils.isEmpty(glnManufacturer) || glnManufacturer.length() != 13) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("%s file GLN manufacturer is empty or has invalid size", fileName)));
        }

        String ean = body.getArticleNumber();
        if (isSlsFile && StringUtils.isEmpty(ean)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("%s file Article number is empty", fileName)));
        }

        String dateAndTime = body.getDateAndTime();
        if (dateAndTime.length() == 17) {
            ofNullable(validateDateTime(fileName, dateAndTime, importType, BODY_DATE_TIME_FORMATTER_FOURTH))
                    .ifPresent(failedChecks::add);
        } else if (dateAndTime.length() == 16) {
            ofNullable(validateDateTime(fileName, dateAndTime, importType, BODY_DATE_TIME_FORMATTER))
                    .ifPresent(failedChecks::add);
        } else if (dateAndTime.length() == 15) {
            ofNullable(validateDateTime(fileName, dateAndTime, importType, BODY_DATE_TIME_FORMATTER_SECOND))
                    .ifPresent(failedChecks::add);
        } else {
            ofNullable(validateDateTime(fileName, dateAndTime, importType, BODY_DATE_TIME_FORMATTER_THIRD))
                    .ifPresent(failedChecks::add);
        }

        ofNullable(validateEmptyField(fileName, body.getBatchId(), importType, "Batch ID"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, body.getCameraNumber(), importType, "Camera number"))
                .ifPresent(failedChecks::add);

        ofNullable(validateEmptyField(fileName, body.getEjectionStationNo(), importType, "Ejection station number"))
                .ifPresent(failedChecks::add);

        ofNullable(validateEmptyField(fileName, body.getDepositAmount(), importType, "Deposit amount"))
                .ifPresent(failedChecks::add);

        String materialType = body.getTypeOfMaterial();
        if (MaterialTypeCode.getAACodeList().stream().noneMatch(materialType::equals)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("%s fileIncorrect type of material", fileName)));
        }

        return failedChecks;
    }

    private List<ImportMessage> checkBatchBody(Pair<String, BatchBody> fileNameAndBatchBody, int index) {
        List<ImportMessage> failedChecks = new ArrayList<>();

        BatchBody batchBody = fileNameAndBatchBody.getRight();
        String fileName = fileNameAndBatchBody.getLeft();
        ImportType importType = batchBody.getImportType();
        ofNullable(validateEmptyField(fileName, batchBody.getBatchId(), importType, "Batch ID amount"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getReferenceNumber(), importType, "Reference number"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getUser(), importType, "User"))
                .ifPresent(failedChecks::add);
        String batchTimeStart = batchBody.getBatchTimeStart();
        if (batchTimeStart.length() == 17) {
            ofNullable(validateDateTime(fileName, batchTimeStart, importType, BODY_DATE_TIME_FORMATTER_FOURTH))
                    .ifPresent(failedChecks::add);
        } else if (batchTimeStart.length() == 16) {
            ofNullable(validateDateTime(fileName, batchTimeStart, importType, BODY_DATE_TIME_FORMATTER))
                    .ifPresent(failedChecks::add);
        } else if (batchTimeStart.length() == 15) {
            ofNullable(validateDateTime(fileName, batchTimeStart, importType, BODY_DATE_TIME_FORMATTER_SECOND))
                    .ifPresent(failedChecks::add);
        } else {
            ofNullable(validateDateTime(fileName, batchTimeStart, importType, BODY_DATE_TIME_FORMATTER_THIRD))
                    .ifPresent(failedChecks::add);
        }
        ofNullable(validateEmptyField(fileName, batchBody.getNumberOfRefundable(), importType, "Number of refundable"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getNumberOfRefundableFromNoReadTable(), importType, "Number of refundable from No-read table"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getNumberOfNonRefundable(), importType, "Number of non-refundable"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getNumberOfNonRefundableFromNoReadTable(), importType, "Number of non-refundable from No-read table"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getEanNotReadable(), importType, "EAN not readable"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getUnknown(), importType, "Unknown"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getNumberInBatch(), importType, "Number in batch"))
                .ifPresent(failedChecks::add);
        ofNullable(validateEmptyField(fileName, batchBody.getNumberInShift(), importType, "Number in shift"))
                .ifPresent(failedChecks::add);
        tryParseTime(batchBody.getBatchTime(), BATCH_TIME_FORMATTER);
        ofNullable(validateEmptyField(fileName, batchBody.getKeyId(), importType, "Key ID"))
                .ifPresent(failedChecks::add);
        ofNullable(validateFieldWithValue(fileName, batchBody.getAnkerAndersenILNNumber(), importType, "5790001396978", "Anker Andersen ILN number"))
                .ifPresent(failedChecks::add);
        ofNullable(validateDate(fileName, batchBody.getPricatVersion(), importType, HEADER_DATE_FORMATTER))
                .ifPresent(failedChecks::add);
        /*ofNullable(validateEmptyField(batchBody.getTotalDepositAmount(), importType, "Total deposit amount"))
                .ifPresent(failedChecks::add);*/ //TODO: not sure if deposit amount is needed.

        return failedChecks;
    }

    private List<ImportMessage> validateMissingImportTypes(Path... files) throws IOException {
        List<ImportMessage> importMessages = new ArrayList<>();
        for (Path file : files) {
            importMessages.addAll(checkMissingImportTypes(file, true));
        }
        return importMessages;
    }

    private ImportMessage validateEmptyField(String fileName, String fieldValue, ImportType importType, String fieldName) {
        return StringUtils.isEmpty(fieldValue) ?
                new ImportMessage(importType.getLineNumber(), String.format("%s file field %s is empty", fileName, fieldName)) :
                null;
    }

    private ImportMessage validateFieldWithValue(String fileName, String fieldValue, ImportType importType, String value, String fieldName) {
        return StringUtils.isEmpty(fieldValue) || !value.equals(fieldValue) ?
                new ImportMessage(importType.getLineNumber(), String.format("%s file field %s has incorrect value", fileName, fieldName)) :
                null;
    }

    private ImportMessage validateDateTime(String fileName, String value, ImportType importType, DateTimeFormatter formatter) {
        LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
        return dateTime.isAfter(LocalDateTime.now()) ?
                new ImportMessage(importType.getLineNumber(),
                        fileName + " file date is in future " + dateTime.format(READABLE_DATE_PATTERN) +
                                ", expected date before: " + LocalDateTime.now().format(READABLE_DATE_PATTERN)) :
                null;

    }

    private ImportMessage validateDate(String fileName, String value, ImportType importType, DateTimeFormatter formatter) {
        LocalDate date = LocalDate.parse(value, formatter);
        return date.isAfter(LocalDate.now()) ?
                new ImportMessage(importType.getLineNumber(),
                        fileName + " file date is in future " + date.format(READABLE_DATE_PATTERN) +
                                ", expected date before: " + LocalDateTime.now().format(READABLE_DATE_PATTERN)) :
                null;
    }

    private void tryParseTime(String value, DateTimeFormatter formatter) {
        LocalTime.parse(value, formatter);
    }

    private List<ImportMessage> validateHashFile(Path file, Path hashFile) throws IOException {
        List<ImportMessage> failedChecks = new ArrayList<>();
        if (!FileUtils.compareSha256HexFromHashFile(file, hashFile)) {
            String message = String.format("Moving AA files to rejected folder, because hash of %s file is wrong or missing in hash file", file.getFileName().toString());
            log.warn(message);
            failedChecks.add(new ImportMessage(0, message));
        }
        return failedChecks;
    }

    private double convertGramsToKilograms(double valueInGrams) {
        return valueInGrams / 1000;
    }

    private double countWeight(SrnArticle article) {
        return article.getWeight() + (0.1 * article.getVolume());
    }

    private double getMaxAllowedWeight(double weight) {
        return 1.5 * weight;
    }
}
