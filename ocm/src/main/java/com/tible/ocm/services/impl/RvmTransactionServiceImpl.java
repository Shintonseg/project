package com.tible.ocm.services.impl;

import com.tible.hawk.core.controllers.helpers.MailData;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.file.FileContent;
import com.tible.ocm.dto.file.FileFooter;
import com.tible.ocm.dto.file.TransactionBody;
import com.tible.ocm.dto.file.TransactionHeader;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.CsvRecordType;
import com.tible.ocm.models.ImportBottlesMessages;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.mongo.*;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.*;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tible.hawk.core.utils.ExportHelper.writeValues;
import static com.tible.hawk.core.utils.ImportHelper.readFileWithLineNumber;
import static com.tible.ocm.models.CommunicationType.AH_CLOUD;
import static com.tible.ocm.models.CommunicationType.AH_TOMRA;
import static com.tible.ocm.services.impl.CompanyServiceImpl.CHARITY_TYPE;
import static com.tible.ocm.services.log.LogKeyConstant.COMMUNICATION_KEY;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;
import static com.tible.ocm.utils.ImportedFileValidationHelper.*;

@Slf4j
@Primary
@Service
public class RvmTransactionServiceImpl implements RvmTransactionService {

    @Value("#{'${mail-to.file-import-failed}'.split(',')}")
    private List<String> fileImportFailedMailTo;

    private final TransactionRepository transactionRepository;
    private final DirectoryService directoryService;
    private final TransactionService transactionService;
    private final SrnArticleService srnArticleService;
    private final ExistingTransactionService existingTransactionService;
    private final ExistingBagService existingBagService;
    private final LabelOrderService labelOrderService;
    private final CompanyService companyService;
    private final LogExporterService<LogFileInfo> loggerExporterService;
    private final ImporterRuleService importerRuleService;
    private final RejectedTransactionService rejectedTransactionService;

    private static final List<String> ALLOWED_BAG_TYPES = List.of("BB", "SB", "CB", "MB");

    public RvmTransactionServiceImpl(TransactionRepository transactionRepository,
                                     DirectoryService directoryService,
                                     TransactionService transactionService,
                                     SrnArticleService srnArticleService,
                                     ExistingTransactionService existingTransactionService,
                                     ExistingBagService existingBagService,
                                     LabelOrderService labelOrderService,
                                     CompanyService companyService,
                                     LogExporterService<LogFileInfo> loggerExporterService,
                                     ImporterRuleService importerRuleService,
                                     RejectedTransactionService rejectedTransactionService) {
        this.transactionRepository = transactionRepository;
        this.directoryService = directoryService;
        this.transactionService = transactionService;
        this.srnArticleService = srnArticleService;
        this.existingTransactionService = existingTransactionService;
        this.existingBagService = existingBagService;
        this.labelOrderService = labelOrderService;
        this.companyService = companyService;
        this.loggerExporterService = loggerExporterService;
        this.importerRuleService = importerRuleService;
        this.rejectedTransactionService = rejectedTransactionService;
    }

    @Override
    public void backupTransactionFile(String number, String version, Path file) {
        final Path backup = directoryService.getTransactionsBackupPath();
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(backup)) {
            log.error("Creating backup directory failed");
        }

        final Path parent = file.getParent();
        final Path hashFile = parent.resolve(getFilename(file) + ".hash");
        copyIfExists(backup, file, hashFile);
    }

    @Override
    public void processTransactionFile(Company company, Path file,
                                       boolean moveFailedToCompanyRejectedDirectory, boolean saveTransaction) {
        final Path rejected = directoryService.getTransactionsRejectedPath();
        final Path failed = directoryService.getTransactionsFailedPath();
        final Path alreadyExists = directoryService.getTransactionsAlreadyExistsPath();
        final Path rejectedCompany = rejected.resolve(company.getNumber());
        final Path failedCompany = failed.resolve(company.getIpAddress());
        final Path companyIpPath = directoryService.getRoot().resolve(company.getIpAddress());

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(rejected)) {
            log.error("Creating rejected directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(failed)) {
            log.error("Creating failed directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(alreadyExists)) {
            log.error("Creating alreadyExists directory failed");
        }

        final Path accepted = directoryService.getTransactionsAcceptedPath();
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(accepted)) {
            log.error("Creating accepted directory failed");
        }

        final Path parent = file.getParent();
        final Path hashFile = parent.resolve(getFilename(file) + ".hash");
        final Path errorFile = parent.resolve(getFilename(file) + ".error");
        LogFileInfo logFileInfo = LogFileInfo.builder()
                .fileName(getFilename(file))
                .path(directoryService.getTransactionLogPath()).build();
        Map<String, Object> contentMap = new HashMap<>(Map.of(COMMUNICATION_KEY, company.getCommunication()));

        try {
            if (!Files.exists(hashFile) || !FileUtils.compareSha256HexFromHashFile(file, hashFile)) {
                log.warn("Moving {} transaction files to rejected folder, because hash is wrong or missing", file);
                if (Files.exists(hashFile)) {
                    log.warn("File hash is: {}", FileUtils.getSha256HexFromFile(file));
                }
                OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);
                List<ImportMessage> importMessages = buildImportMessages(String.format("Moving %s transaction file to rejected folder, because hash is wrong or missing", file.getFileName().toString()));
                createErrorFile(file.getFileName().toString(), errorFile, importMessages);
                saveRejectedTransaction(getFilename(file), company.getNumber());
                loggerExporterService.exportWithContentMap(file, contentMap, importMessages, logFileInfo, company, false, IMPORT_STATUS_REJECTED);

                processWorkWithErrorFile(companyIpPath, TRANS_DIRECTORY, moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, errorFile);
                return;
            }

            if (Files.exists(accepted.resolve(getFilename(file) + "-" + company.getNumber() + ".csv"))) {
                log.warn("Moving {} transaction files to already exists folder, because transaction file is already processed and accepted", file.toString());
                OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);
                // List<ImportMessage> importMessages = buildImportMessages(String.format("Moving %s transaction file to rejected folder, because transaction file is already processed and accepted", file.getFileName().toString()));
                // createErrorFile(file.getFileName().toString(), errorFile, importMessages);

                loggerExporterService.exportWithContentMap(file, contentMap, List.of(), logFileInfo, company, true, IMPORT_STATUS_ALREADY_EXISTS);

                //processWorkWithErrorFile(companyPath, TRANS_DIRECTORY, moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, errorFile);
                if (company.isNotifyAboutDoubleTransactions()) {
                    moveIfExists(rejectedCompany, file, hashFile);
                } else {
                    moveIfExists(alreadyExists, file, hashFile);
                }
                return;
            }

            if (existingTransactionService.lazyCheckIsTransactionAlreadyExists(getFilename(file), company.getRvmOwnerNumber())) {
                log.warn("Moving {} transaction files to already exists folder, because transaction file is already processed and saved at SNL", file);
                OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);
                // List<ImportMessage> importMessages = buildImportMessages(String.format("Moving %s transaction file to rejected folder, because transaction file is already processed and saved at SRN", file.getFileName().toString()));
                // createErrorFile(file.getFileName().toString(), errorFile, importMessages);

                loggerExporterService.exportWithContentMap(file, contentMap, List.of(), logFileInfo, company, true, IMPORT_STATUS_ALREADY_EXISTS);

                // processWorkWithErrorFile(companyPath, TRANS_DIRECTORY, moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, errorFile);
                if (company.isNotifyAboutDoubleTransactions()) {
                    moveIfExists(rejectedCompany, file, hashFile);
                } else {
                    moveIfExists(alreadyExists, file, hashFile);
                }
                return;
            }

            // Check if transaction is unique
            if (transactionRepository.existsByTransactionNumber(getFilename(file))) {
                log.warn("Moving {} transaction files to already exists folder, because transaction file is already processed", file.toString());
                OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);

                loggerExporterService.exportWithContentMap(file, contentMap, List.of(), logFileInfo, company, true, IMPORT_STATUS_ALREADY_EXISTS);

                if (company.isNotifyAboutDoubleTransactions()) {
                    moveIfExists(rejectedCompany, file, hashFile);
                } else {
                    moveIfExists(alreadyExists, file, hashFile);
                }
                return;
            }

            List<ImportMessage> failedChecks = new ArrayList<>();
            if (getFilename(file).length() != 21) {
                failedChecks.add(new ImportMessage(0,
                        "File name of " + getFilename(file) + " is not 21 characters"));
            }

            failedChecks.addAll(checkMissingImportTypes(file, false));
            Pair<List<ImportMessage>, FileContent<TransactionHeader, TransactionBody>> importMessagesAndFileContent = readAndCheckTransactionFile(company, file);
            failedChecks.addAll(importMessagesAndFileContent.getLeft());
            if (!failedChecks.isEmpty()) {
                MailData mailData = createMail("OCM one or more of the import checks for file " + file.getFileName().toString() + " failed",
                        file.getFileName().toString(), failedChecks, fileImportFailedMailTo);
                // mailService.sendMail(mailData, null); // Turned off for now

                log.warn("Moving {} transaction files to rejected folder, because one or more import checks failed", file.toString());
                OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);

                loggerExporterService.exportWithContentMap(file, contentMap, failedChecks, logFileInfo, company, false, IMPORT_STATUS_REJECTED);

                createErrorFile(file.getFileName().toString(), errorFile, failedChecks);
                saveRejectedTransaction(getFilename(file), company.getNumber());
                processWorkWithErrorFile(companyIpPath, TRANS_DIRECTORY, moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, errorFile);
                return;
            }

            String newFileName = getFilename(file) + "-" + company.getNumber() + ".csv";
            String newHashFileName = getFilename(hashFile) + "-" + company.getNumber() + ".hash";
            if (saveTransaction) {
                saveTransactionFile(company.getNumber(), getFilename(file), importMessagesAndFileContent.getRight());

                final Path backup = directoryService.getTransactionsBackupPath();
                if (OcmFileUtils.checkOrCreateDirWithFullPermissions(backup)) {
                    Path backupIpPath = backup.resolve(company.getIpAddress());
                    if (OcmFileUtils.checkOrCreateDirWithFullPermissions(backupIpPath)) {
                        moveIfExists(backupIpPath, file);
                        moveIfExists(backupIpPath, hashFile);
                    }
                }
            } else {
                moveAndRenameIfExists(accepted, file, newFileName);
                Files.deleteIfExists(hashFile);
                createTransactionHashFile(accepted.resolve(newFileName), accepted.resolve(newHashFileName));
            }

            loggerExporterService.exportWithContentMap(file, contentMap, failedChecks, logFileInfo, company, false, IMPORT_STATUS_ACCEPTED);
        } catch (NoSuchElementException e) {
            log.warn("Moving {} transaction files to rejected folder, because one of the files is missing a field", file.toString(), e);
            OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedCompany);
            List<ImportMessage> importMessages = buildImportMessages(String.format("Moving %s transaction file to rejected folder, because one of the files is missing a field", file.getFileName().toString()));
            createErrorFile(file.getFileName().toString(), errorFile, importMessages);
            saveRejectedTransaction(getFilename(file), company.getNumber());
            loggerExporterService.exportWithContentMap(file, contentMap, importMessages, logFileInfo, company, false, IMPORT_STATUS_REJECTED);

            processWorkWithErrorFile(companyIpPath, TRANS_DIRECTORY, moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, errorFile);
        } catch (Exception e) {
            log.warn("Moving {} transaction files to failed folder, because of an error", file.toString(), e);
            OcmFileUtils.checkOrCreateDirWithFullPermissions(failedCompany);
            List<ImportMessage> importMessages = buildImportMessages(String.format("Moving %s transaction file to failed folder, because the next error: %s", file.getFileName().toString(), e.getMessage()));
            createErrorFile(file.getFileName().toString(), errorFile, importMessages);

            loggerExporterService.exportWithContentMap(file, contentMap, importMessages, logFileInfo, company, false, IMPORT_STATUS_FAILED);

            moveIfExists(failedCompany, file, hashFile, errorFile);
        }

    }

    @Override
    public void processTransactionBackupOrFailedFiles(Company company, Path csvPath, boolean failedDir) {
        final Path companyTransPath = directoryService.getRoot().resolve(company.getIpAddress()).resolve(TRANS_DIRECTORY);

        final Path parent = csvPath.getParent();
        final Path hashPath = parent.resolve(getFilename(csvPath) + ".hash");

        if (failedDir) {
            moveIfExists(companyTransPath, csvPath, hashPath);
        } else {
            copyIfExists(companyTransPath, csvPath, hashPath);
        }
    }

    private void saveTransactionFile(String number, String transactionNumber, FileContent<TransactionHeader, TransactionBody> fileContent) {
        TransactionHeader transactionHeader = fileContent.getHeader();
        List<TransactionBody> transactionBodyList = fileContent.getBody();
        FileFooter fileFooter = fileContent.getFooter();

        Transaction transaction = new Transaction();
        transaction.setVersion(transactionHeader.getVersion());
        transaction.setDateTime(LocalDateTime.parse(transactionHeader.getDateTime(), DATETIMEFORMATTER));
        transaction.setStoreId(transactionHeader.getStoreId());
        transaction.setSerialNumber(transactionHeader.getRvmSerial());
        if (transactionHeader.getLabelNumber() != null && !transactionHeader.getLabelNumber().isEmpty()) {
            transaction.setLabelNumber(transactionHeader.getLabelNumber());
        }
        if (transactionHeader.getBagType() != null && !transactionHeader.getBagType().isEmpty()) {
            transaction.setBagType(transactionHeader.getBagType());
        }
        if (transactionHeader.getCharityNumber() != null && !transactionHeader.getCharityNumber().isEmpty()) {
            transaction.setCharityNumber(transactionHeader.getCharityNumber());
        }

        transaction.setTransactionNumber(transactionNumber);
        transaction.setTotal(Integer.parseInt(fileFooter.getTotal()));
        transaction.setRefundable(Integer.parseInt(fileFooter.getRefunded()));
        transaction.setCollected(Integer.parseInt(fileFooter.getCollected()));
        if (fileFooter.getManual() != null && !fileFooter.getManual().isEmpty()) {
            transaction.setManual(Integer.parseInt(fileFooter.getManual()));
        }
        if (fileFooter.getRejected() != null && !fileFooter.getRejected().isEmpty()) {
            transaction.setRejected(Integer.parseInt(fileFooter.getRejected()));
        }
        transaction.setType(FILE_TYPE_TRANSACTION);
        transaction.setReceivedDate(LocalDateTime.now());


        List<TransactionArticle> transactionArticles = new ArrayList<>();
        transactionBodyList.forEach(transactionBody -> transactionArticles.add(processTransactionBody(transactionBody)));

        transactionService.saveTransactionAndArticlesByCompany(transaction, transactionArticles, companyService.findByNumber(number));
    }

    private TransactionArticle processTransactionBody(TransactionBody transactionBody) {
        TransactionArticle transactionArticle = new TransactionArticle();
        transactionArticle.setArticleNumber(transactionBody.getArticleNumber());
        if (transactionBody.getScannedWeight() != null && !transactionBody.getScannedWeight().isEmpty()) {
            transactionArticle.setScannedWeight(Integer.parseInt(transactionBody.getScannedWeight()));
        }
        if (transactionBody.getMaterial() != null && !transactionBody.getMaterial().isEmpty()) {
            transactionArticle.setMaterial(Integer.parseInt(transactionBody.getMaterial()));
        }
        transactionArticle.setRefund(Integer.parseInt(transactionBody.getRefunded()));
        transactionArticle.setCollected(Integer.parseInt(transactionBody.getCollected()));
        if (transactionBody.getManual() != null && !transactionBody.getManual().isEmpty()) {
            transactionArticle.setManual(Integer.parseInt(transactionBody.getManual()));
        }

        return transactionArticle;
    }

    private void moveToRejectedCompanyDirectory(Path file, Path hashFile, Path errorFile, String ipAddress) {
        Path companyPath = directoryService.getRoot().resolve(ipAddress);
        Path companyTransRejectedPath = companyPath.resolve(TRANS_DIRECTORY).resolve(REJECTED_DIRECTORY);
        moveIfExists(companyTransRejectedPath, file, hashFile, errorFile);
    }

    private Pair<List<ImportMessage>, FileContent<TransactionHeader,
            TransactionBody>> readAndCheckTransactionFile(Company company, Path file) throws IOException {
        List<ImportMessage> failedChecks = new ArrayList<>();
        FileContent<TransactionHeader, TransactionBody> fileContent = new FileContent<>();
        ArrayList<TransactionBody> fileBody = new ArrayList<>();

        List<SrnArticle> articles = srnArticleService.getAll();
        readFileWithLineNumber(file, (scanner, importType) -> {
            switch (CsvRecordType.valueOf(importType.getType())) {
                case HDR:
                    TransactionHeader transactionHeader = TransactionHeader.header(scanner, company.getVersion(), importType);
                    fileContent.setHeader(transactionHeader);
                    break;
                case POS:
                    TransactionBody transactionBody = TransactionBody.body(scanner, company.getVersion(), importType);
                    fileBody.add(transactionBody);
                    break;
                case SUM:
                    FileFooter fileFooter = FileFooter.footer(scanner, company.getVersion(), importType);
                    fileContent.setFooter(fileFooter);
                    break;
            }
        });
        fileContent.setBody(fileBody);

        AtomicInteger posCount = new AtomicInteger(0);
        AtomicInteger refundedSum = new AtomicInteger(0);
        AtomicInteger collectedSum = new AtomicInteger(0);
        AtomicInteger manualSum = new AtomicInteger(0);
        failedChecks.addAll(checkTransactionHeader(company, fileContent.getHeader()));
        fileContent.getBody().forEach(transactionBody -> {
            ImportBottlesMessages importBottlesMessages = checkTransactionBottles(company, transactionBody, fileContent.getHeader(), articles);
            failedChecks.addAll(importBottlesMessages.getMessages());
            if (importBottlesMessages.isRefunded()) {
                refundedSum.getAndAdd(1);
            }
            if (importBottlesMessages.isCollected()) {
                collectedSum.getAndAdd(1);
            }
            if (importBottlesMessages.isManual()) {
                manualSum.getAndAdd(1);
            }
            posCount.getAndAdd(1);
        });
        failedChecks.addAll(checkTransactionSum(company, fileContent.getFooter(), posCount, refundedSum, collectedSum,
                manualSum));

        return Pair.of(failedChecks, fileContent);
    }

    private List<ImportMessage> checkTransactionHeader(Company company, TransactionHeader transactionHeader) {
        ImportType importType = transactionHeader.getImportType();
        List<ImportMessage> failedChecks;

        String transactionStoreId = transactionHeader.getStoreId(); // message sender (store id)
        int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
        if (!StringUtils.isEmpty(transactionStoreId) && companyService.existsByStoreIdAndRvmOwnerNumber(transactionStoreId, company.getRvmOwnerNumber())) {
            Company companyStoreId = companyService.findByStoreIdAndRvmOwnerNumber(transactionStoreId, company.getRvmOwnerNumber());
            failedChecks = checkFileContentHeader(company.getNumber(), companyStoreId != null ? companyStoreId.getVersion() : null,
                    transactionHeader, importType, dataExpirationPeriodInDays);
        } else {
            failedChecks = checkFileContentHeader(company.getNumber(), company.getVersion(),
                    transactionHeader, importType, dataExpirationPeriodInDays);
        }

        if (StringUtils.isEmpty(transactionStoreId)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "StoreID field is empty"));
        } else {
            if (!company.isUsingIpTrunking() && company.getStoreId() != null && !company.getStoreId().equals(transactionStoreId)) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "StoreID " + transactionStoreId + " is not valid"));
            } else if (!company.isUsingIpTrunking() && company.getStoreId() == null) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "StoreID " + transactionStoreId + " is not valid"));
            }
        }

        String rvmSerial = transactionHeader.getRvmSerial(); // message machine (rvm serial)
        if (StringUtils.isEmpty(rvmSerial)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Rvm serial field is empty"));
        } else {
            if (company.isUsingIpTrunking()) {
                if (!StringUtils.isEmpty(transactionStoreId) && companyService.existsByStoreIdAndRvmOwnerNumber(transactionStoreId, company.getRvmOwnerNumber())) {
                    Company companyStoreId = companyService.findByStoreIdAndRvmOwnerNumber(transactionStoreId, company.getRvmOwnerNumber());
                    if (companyStoreId.getSerialNumbers().stream().noneMatch(rvmSerial::equals)) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(),
                                "StoreID does not have the following rvm serial " + rvmSerial));
                    }
                } else {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "StoreID is missing at OCM"));
                }
            } else {
                if (company.getSerialNumbers().stream().noneMatch(rvmSerial::equals)) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "RVM Owner does not have the following rvm serial " + rvmSerial));
                }
            }
        }

        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            Company companyByStoreId = companyService.findByStoreIdAndRvmOwnerNumber(transactionHeader.getStoreId(), company.getRvmOwnerNumber());
            if (!StringUtils.isEmpty(transactionHeader.getLabelNumber())) {
                if (!StringUtils.isNumeric(transactionHeader.getLabelNumber())) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Number (label) is not numeric " + transactionHeader.getLabelNumber()));
                } else if (transactionHeader.getLabelNumber().length() > 17) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Number (label) is longer than 17 characters " + transactionHeader.getLabelNumber()));
                }

                // Check transaction bag label already exists at SRN existingBagService
                if (existingBagService.existsByCombinedCustomerNumberLabel(transactionHeader.getLabelNumber())) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(), "Label number " + transactionHeader.getLabelNumber() + " is already in use"));
                }

                // Check transaction bag label already exists at SRN existingTransactionService
                if (existingTransactionService.existsByCombinedCustomerNumberLabel(transactionHeader.getLabelNumber())) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(), "Label number " + transactionHeader.getLabelNumber() + " is already in use"));
                }

                // Check if transaction bag label is unique
                if (transactionRepository.existsByLabelNumber(transactionHeader.getLabelNumber())) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(), String.format("Transaction with label number %s already exist.", transactionHeader.getLabelNumber())));
                }

                String customerNumberFromLabel = getCustomerNumberFromLabel(transactionHeader.getLabelNumber());
                Company customerByNumber = companyService.findByNumber(customerNumberFromLabel);
                Integer numberFromLabel = getLabelNumberFromLabel(transactionHeader.getLabelNumber());
                if (customerByNumber != null) {
                    if (customerByNumber.getRvmOwnerNumber() != null) {
                        if (!labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                customerNumberFromLabel, company.getRvmOwnerNumber(), Long.valueOf(numberFromLabel))) {
                            failedChecks.add(new ImportMessage(importType.getLineNumber(), "Label number " + transactionHeader.getLabelNumber() + " is using the wrong label number for the wrong customer number"));
                        }
                    } else {
                        if (!labelOrderService.existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                customerNumberFromLabel, Long.valueOf(numberFromLabel))) {
                            failedChecks.add(new ImportMessage(importType.getLineNumber(), "Label number " + transactionHeader.getLabelNumber() + " is using the wrong label number for the wrong customer number"));
                        }
                    }
                } else {
                    if (!labelOrderService.existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                            customerNumberFromLabel, Long.valueOf(numberFromLabel))) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(), "Label number " + transactionHeader.getLabelNumber() + " is using the wrong label number for the wrong customer number"));
                    }
                }

                if (!StringUtils.isEmpty(transactionHeader.getBagType())) {
                    if (transactionHeader.getBagType().length() != 2) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(),
                                "Bag type is not 2 characters long"));
                    } else if (!ALLOWED_BAG_TYPES.contains(transactionHeader.getBagType())) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(),
                                "Bag type is not using the right 2 characters"));
                    }
                }
            } else {
                if (companyByStoreId != null && companyByStoreId.getType().equals("DISTRIBUTION_CENTER") && (company.getCommunication().equals(AH_CLOUD) || company.getCommunication().equals(AH_TOMRA))) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(), "Need to use a label number when using a store id from a distribution center"));
                }
            }
        }

        if (ImportedFileValidationHelper.version17Check(company.getVersion())) {
            if (!StringUtils.isEmpty(transactionHeader.getCharityNumber())) {
                if (!companyService.existsByTypeAndNumber(CHARITY_TYPE, transactionHeader.getCharityNumber())) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(), "Charity number " + transactionHeader.getCharityNumber() + " does not exist"));
                }
            }
        }

        return failedChecks;
    }

    private ImportBottlesMessages checkTransactionBottles(Company company, TransactionBody transactionBody,
                                                          TransactionHeader transactionHeader, List<SrnArticle> articles) {
        ImportBottlesMessages importBottlesMessages = new ImportBottlesMessages();
        List<ImportMessage> failedChecks = new ArrayList<>();
        ImportType importType = transactionBody.getImportType();
        List<ImporterRule> importerRuleEans = importerRuleService.getAllByRvmOwnerAndRvmSerial(company.getRvmOwnerNumber(), List.of(transactionHeader.getRvmSerial()));

        String ean;
        String eanText;
        Optional<ImporterRule> importerRuleOptional = importerRuleEans
                .stream()
                .filter(importerRule -> importerRule.getFromEan().equals(transactionBody.getArticleNumber()))
                .findFirst();
        if (importerRuleOptional.isPresent()) {
            ean = importerRuleOptional.get().getToEan();
            eanText = transactionBody.getArticleNumber() + " (" + importerRuleOptional.get().getToEan() + ")";
        } else {
            ean = transactionBody.getArticleNumber(); // article number
            eanText = transactionBody.getArticleNumber();
        }

        String scannedWeight = transactionBody.getScannedWeight(); // scanned weight
        if (!StringUtils.isEmpty(scannedWeight)) {
            int scannedWeightInt = Integer.parseInt(scannedWeight);
            Optional<SrnArticle> article = articles.stream().filter(a -> a.getNumber().equals(ean)).findFirst();
            /*if (article.isPresent() && article.get().getWeight() > scannedWeightInt) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Scanned weight cannot be less then article weight."));
            }*/
            if (article.isPresent()) {
                double srnArticleMinWeight = article.get().getWeight();
                if (scannedWeightInt < srnArticleMinWeight) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            String.format("Article with number %s scanned weight is lower than expected weight.", eanText)));
                }

                double srnArticleMaxWeight = srnArticleMinWeight + (article.get().getVolume() * 1000 * 0.10); // 10% of volume of article in milligrams.
                if (scannedWeightInt > srnArticleMaxWeight) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            String.format("Article with number %s scanned weight is higher than expected weight.", eanText)));
                }
            }
        }

        if (ImportedFileValidationHelper.version15Check(company.getVersion())) {
            String material = transactionBody.getMaterial(); // material
            if (StringUtils.isEmpty(material)) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(), "Material field is empty"));
            } else {
                int materialInt = Integer.parseInt(material);
                if (materialInt < 1 || materialInt > 4) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Material field is " + materialInt + ", expected value between 1 and 4"));
                }

                Optional<SrnArticle> article = articles.stream().filter(a -> a.getNumber().equals(ean)).findFirst();
                if (article.isPresent()) {
                    Integer srArticleMaterial = article.get().getMaterial();
                    if (srArticleMaterial != null && srArticleMaterial != materialInt) {
                        failedChecks.add(new ImportMessage(importType.getLineNumber(),
                                String.format("Article with number %s material is different than expected material.", eanText)));
                    }
                }
            }
        }

        String refunded = transactionBody.getRefunded(); // refunded
        if (StringUtils.isEmpty(refunded)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Refunded field is empty"));
        } else {
            int refundedInt = Integer.parseInt(refunded);
            if (refundedInt < 0 || refundedInt > 1) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Refunded field is " + refundedInt + ", expected: 0 or 1"));
            }

            if (refundedInt == 1) {
                importBottlesMessages.setRefunded(true);
            } else {
                importBottlesMessages.setRefunded(false);
            }
        }

        String collected = transactionBody.getCollected(); // collected
        if (StringUtils.isEmpty(collected)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Collected field is empty"));
        } else {
            int collectedInt = Integer.parseInt(collected);
            if (collectedInt < 0 || collectedInt > 1) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Collected field is " + collectedInt + ", expected: 0 or 1"));
            }

            if (collectedInt == 1) {
                importBottlesMessages.setCollected(true);
            } else {
                importBottlesMessages.setCollected(false);
            }
        }

        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            String manual = transactionBody.getManual(); // manual
            if (StringUtils.isEmpty(manual)) {
                importBottlesMessages.setManual(false);
            } else {
                int manualInt = Integer.parseInt(manual);
                if (manualInt < 0 || manualInt > 1) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Manual field is " + manualInt + ", expected: 0 or 1"));
                }

                if (manualInt == 1) {
                    importBottlesMessages.setManual(true);
                } else {
                    importBottlesMessages.setManual(false);
                }
            }
        }

        LocalDateTime checkDateTime = LocalDateTime.parse(transactionHeader.getDateTime(), DATETIMEFORMATTER);
        if (StringUtils.isEmpty(ean)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "EAN field is empty"));
        } else if (articles.stream().noneMatch(article -> article.getNumber().equals(ean)) &&
                importerRuleEans.stream().noneMatch(importerRule -> importerRule.getFromEan().equals(ean))
                && Integer.parseInt(refunded) == 1) { //articles = all ocm db articles
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "The following ean " + ean + " does not exist"));
        } else if (NumberUtils.isParsable(refunded) && NumberUtils.isParsable(collected) && (articles.stream().anyMatch(article -> article.getNumber().equals(ean)) ||
                importerRuleEans.stream().anyMatch(importerRule -> importerRule.getFromEan().equals(ean))) &&
                Integer.parseInt(refunded) == 1 && Integer.parseInt(collected) == 0) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(), "Article with number " + eanText + " is refunded and should be collected."));
        } else if (NumberUtils.isParsable(refunded) && Integer.parseInt(refunded) == 1) {
            if (articles.stream()
                    .anyMatch(article -> article.getNumber().equals(ean) &&
                            article.getFirstArticleActivationDate() == null &&
                            article.getActivationDate() != null && article.getActivationDate().isAfter(checkDateTime))) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "The activation date is in the future for article with number " + eanText));
            } else if (articles.stream()
                    .anyMatch(article -> article.getNumber().equals(ean) && article.getActivationDate() != null &&
                            article.getFirstArticleActivationDate() != null && article.getFirstArticleActivationDate().isBefore(article.getActivationDate()) &&
                            article.getFirstArticleActivationDate().isAfter(checkDateTime))) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "The activation date is in the future for article with number " + eanText));
            }
        }

        importBottlesMessages.setMessages(failedChecks);
        return importBottlesMessages;
    }

    private List<ImportMessage> checkTransactionSum(Company company, FileFooter fileFooter, AtomicInteger posCount,
                                                    AtomicInteger refundedSum, AtomicInteger collectedSum,
                                                    AtomicInteger manualSum) {
        ImportType importType = fileFooter.getImportType();
        List<ImportMessage> failedChecks = checkFileSumFooter(company, fileFooter, importType, posCount);

        String refundedAmount = fileFooter.getRefunded(); // refunded amount
        if (StringUtils.isEmpty(refundedAmount)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Refunded sum amount field is empty"));
        } else {
            if (refundedSum.get() != Integer.parseInt(refundedAmount)) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Total refunded amount of articles is " + refundedSum.get() + ", does not equal refunded sum amount field value: " + Integer.parseInt(refundedAmount)));
            }
        }

        String collectedAmount = fileFooter.getCollected(); // collected amount
        if (StringUtils.isEmpty(collectedAmount)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Collected sum amount field is empty"));
        } else {
            if (collectedSum.get() != Integer.parseInt(collectedAmount)) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Total collected amount of articles is " + collectedSum.get() + ", does not equal collected sum amount field value: " + Integer.parseInt(collectedAmount)));
            }
        }

        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            String manualAmount = fileFooter.getManual(); // manual amount
            if (!StringUtils.isEmpty(manualAmount)) {
                if (manualSum.get() != Integer.parseInt(manualAmount)) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Total manual amount of articles is " + manualSum.get() + ", does not equal manual sum amount field value: " + Integer.parseInt(manualAmount)));
                }
            }
        }

        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            String rejectedAmount = fileFooter.getRejected(); // rejected amount
            if (!StringUtils.isEmpty(rejectedAmount)) {
                // Checks on rejected amount
                if (manualSum.get() > Integer.parseInt(rejectedAmount)) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Manual amount of articles is " + manualSum.get() + ", which is greater than the rejected sum amount field value: " + Integer.parseInt(rejectedAmount)));
                }
                if (Integer.parseInt(rejectedAmount) < 0) {
                    failedChecks.add(new ImportMessage(importType.getLineNumber(),
                            "Rejected sum amount is " + Integer.parseInt(rejectedAmount) + ", which is lower than 0 and not possible"));
                }
            }
        }

        return failedChecks;
    }

    private void createTransactionHashFile(Path transactionFile, Path transactionHashFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(transactionHashFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writeValues(writer, false, FileUtils.getSha256HexFromFile(transactionFile));
        }
    }

    private void saveRejectedTransaction(String fileNameBase, String companyNumber) {
        RejectedTransaction rejectedTransaction = RejectedTransaction
                .builder()
                .baseFileName(fileNameBase)
                .createdAt(LocalDateTime.now())
                .type(RejectedTransaction.TransactionType.TRANSACTION)
                .companyNumber(companyNumber)
                .isExternal(Boolean.FALSE)
                .needToBeDeleted(Boolean.FALSE)
                .build();

        rejectedTransactionService.save(rejectedTransaction);
    }
}
