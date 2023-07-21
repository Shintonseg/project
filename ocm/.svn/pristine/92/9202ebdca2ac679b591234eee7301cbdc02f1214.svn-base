package com.tible.ocm.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.ocm.dto.TransactionArticleDto;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.*;
import com.tible.ocm.rabbitmq.PublisherTransactionImportRest;
import com.tible.ocm.rabbitmq.TransactionFilePayloadRest;
import com.tible.ocm.repositories.mongo.*;
import com.tible.ocm.services.*;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import com.tible.ocm.utils.OcmFileUtils;
import com.tible.ocm.utils.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.*;
import static com.tible.ocm.models.OcmStatus.*;
import static com.tible.ocm.services.impl.CompanyServiceImpl.CHARITY_TYPE;
import static com.tible.ocm.services.log.LogKeyConstant.COMMUNICATION_KEY;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportedFileValidationHelper.createErrorFileForREST;
import static com.tible.ocm.utils.ImportedFileValidationHelper.processWorkWithErrorFile;

@Slf4j
@Primary
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionArticleRepository transactionArticleRepository;
    private final SrnArticleService srnArticleService;
    private final CompanyService companyService;
    private final ConversionService conversionService;
    private final ExistingTransactionService existingTransactionService;
    private final ImporterRuleService importerRuleService;
    private final DirectoryService directoryService;
    private final LogExporterService<LogFileInfo> loggerExporterService;
    private final ExistingBagService existingBagService;
    private final LabelOrderService labelOrderService;
    private final EnvironmentService environmentService;
    private final ObjectMapper objectMapper;
    private final PublisherTransactionImportRest publisherTransactionImportRest;
    private final LabelOrderRepository labelOrderRepository;
    private final ExistingBagLatestRepository existingBagLatestRepository;
    private final ExistingTransactionLatestRepository existingTransactionLatestRepository;
    private final ImporterRuleRepository importerRuleRepository;

    public final Integer restTransactionsHandlingLimit;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  TransactionArticleRepository transactionArticleRepository,
                                  SrnArticleService srnArticleService,
                                  CompanyService companyService,
                                  ConversionService conversionService,
                                  ExistingTransactionService existingTransactionService,
                                  ImporterRuleService importerRuleService,
                                  DirectoryService directoryService,
                                  LogExporterService<LogFileInfo> loggerExporterService,
                                  ExistingBagService existingBagService,
                                  LabelOrderService labelOrderService,
                                  EnvironmentService environmentService,
                                  ObjectMapper objectMapper,
                                  PublisherTransactionImportRest publisherTransactionImportRest,
                                  LabelOrderRepository labelOrderRepository,
                                  ExistingBagLatestRepository existingBagLatestRepository,
                                  ExistingTransactionLatestRepository existingTransactionLatestRepository,
                                  ImporterRuleRepository importerRuleRepository,
                                  @Value("${transaction.rest-handling-limit:1000}") Integer restTransactionsHandlingLimit) {
        this.transactionRepository = transactionRepository;
        this.transactionArticleRepository = transactionArticleRepository;
        this.srnArticleService = srnArticleService;
        this.companyService = companyService;
        this.conversionService = conversionService;
        this.existingTransactionService = existingTransactionService;
        this.importerRuleService = importerRuleService;
        this.directoryService = directoryService;
        this.loggerExporterService = loggerExporterService;
        this.existingBagService = existingBagService;
        this.labelOrderService = labelOrderService;
        this.environmentService = environmentService;
        this.objectMapper = objectMapper;
        this.publisherTransactionImportRest = publisherTransactionImportRest;
        this.restTransactionsHandlingLimit = restTransactionsHandlingLimit;
        this.labelOrderRepository = labelOrderRepository;
        this.existingBagLatestRepository = existingBagLatestRepository;
        this.existingTransactionLatestRepository = existingTransactionLatestRepository;
        this.importerRuleRepository = importerRuleRepository;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsPath())) {
            log.error("Creating transaction directory failed");
        }
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Override
    public OcmTransactionResponse handleTransaction(TransactionDto transactionDto, String ipAddress) {
        ipAddress = ipAddress.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ipAddress;
        Company company;

//        // Check if the required tables are empty
//        if (areTablesEmpty()) {
//            List<OcmMessage> message = Collections.singletonList(new OcmMessage("Transaction cannot be processed yet because the current OCM is not yet prepared. Please send the transaction again at a later time."));
//            return new OcmTransactionResponse(DECLINED, message, transactionDto.getTransactionNumber());
//        }

        if (environmentService.matchGivenProfiles("dev") && ipAddress.equals("127.0.0.1")) {
            log.info("Using dev environment");
            OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
            String clientId = authentication.getOAuth2Request().getClientId();
            company = companyService.findFirstByIpAddress(clientId);
        } else {
            company = companyService.findFirstByIpAddress(ipAddress);
        }

        if (company == null) {
            List<OcmMessage> message = Collections.singletonList(new OcmMessage("Company does not exist with ip " + ipAddress));
            return new OcmTransactionResponse(DECLINED, message, transactionDto.getTransactionNumber());
        }

        // RvmSupplier rvmSupplier = machine != null ? rvmSupplierRepository.findByRvmMachines(machine.getId()) : null;
        final Path rejected = directoryService.getTransactionsRejectedPath();
        final Path rejectedCompany = rejected.resolve(company.getNumber());
        final Path failed = directoryService.getTransactionsFailedPath();
        final Path failedCompany = failed.resolve(company.getIpAddress());
        final Path companyIpPath = directoryService.getRoot().resolve(company.getIpAddress());
        final Path companyTransPath = companyIpPath.resolve(TRANS_DIRECTORY);
        final Path alreadyExists = directoryService.getTransactionsAlreadyExistsPath();
        final Path accepted = directoryService.getTransactionsAcceptedPath();

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(rejected)) {
            log.error("Creating rejected directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(failed)) {
            log.error("Creating failed directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(alreadyExists)) {
            log.error("Creating alreadyExists directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(accepted)) {
            log.error("Creating accepted directory failed");
        }

        OcmFileUtils.checkOrCreateDirWithFullPermissions(failedCompany);

        LogFileInfo logFileInfo = LogFileInfo.builder()
                .fileName(transactionDto.getTransactionNumber())
                .path(directoryService.getTransactionLogPath()).build();
        Map<String, Object> contentMap = new HashMap<>(Map.of(COMMUNICATION_KEY, company.getCommunication() != null ? company.getCommunication() : REST));
        try {
                // Check if the necessary tables are empty
                if (isTablesEmpty()) {
                    List<OcmMessage> message = Collections.singletonList(new OcmMessage("Transaction cannot be processed yet because the current OCM is not yet prepared. Please send the transaction again at a later time."));
                    return new OcmTransactionResponse(DECLINED, message, transactionDto.getTransactionNumber());
                }

            OcmTransactionResponse response = validateTransaction(transactionDto, company, ipAddress, accepted);

            if (response.getStatus().equals(ACCEPTED)) {
                if (transactionDto.getTotal() > restTransactionsHandlingLimit) {
                    log.info("Transaction has more than {} entries, send transaction to the queue",
                            restTransactionsHandlingLimit);
                    moveTransactionRestToQueue(transactionDto, company);
                    return response;
                }
                Transaction savedTransaction = saveTransaction(transactionDto, company);

                if (savedTransaction == null) {
                    List<OcmMessage> message = Collections.singletonList(new OcmMessage("Failed to save transaction"));
                    log.warn("Failed to save transaction");

                    final Path errorFile = companyTransPath.resolve(response.getTransactionNumber() + ".error");
                    createErrorFileForREST(response.getTransactionNumber(), errorFile, response.getMessages(), transactionDto);
                    moveIfExists(failedCompany, errorFile);

                    loggerExporterService.exportWithContentMap(contentMap, response.getMessages(), response.getTransactionNumber(),
                            logFileInfo, company, false, IMPORT_STATUS_FAILED);
                    return new OcmTransactionResponse(FAILED, message, transactionDto.getTransactionNumber());
                }
            } else {
                final Path errorFile = companyTransPath.resolve(response.getTransactionNumber() + ".error");
                createErrorFileForREST(response.getTransactionNumber(), errorFile, response.getMessages(), transactionDto);
                if (response.getStatus().equals(DECLINED)) {
                    processWorkWithErrorFile(companyIpPath, TRANS_DIRECTORY, true, rejectedCompany, errorFile);
                } else if (response.getStatus().equals(DUPLICATE)) {
                    if (company.isNotifyAboutDoubleTransactions()) {
                        moveIfExists(rejectedCompany, errorFile);
                    } else {
                        moveIfExists(alreadyExists, errorFile);
                    }
                }
            }

            if (response.getStatus().equals(DUPLICATE)) {
                loggerExporterService.exportWithContentMap(contentMap, response.getMessages(), response.getTransactionNumber(),
                        logFileInfo, company, true, IMPORT_STATUS_ALREADY_EXISTS);
            } else {
                loggerExporterService.exportWithContentMap(contentMap, response.getMessages(), response.getTransactionNumber(),
                        logFileInfo, company, false, response.getStatus().equals(ACCEPTED) ? IMPORT_STATUS_ACCEPTED : IMPORT_STATUS_REJECTED);
            }

            return response;
        } catch (Exception e) {
            log.warn("Failed to process transaction, because of an exception", e);
            List<OcmMessage> message = Collections.singletonList(new OcmMessage("Exception: " + ExceptionUtils.getStackTrace(e)));

            final Path errorFile = companyTransPath.resolve(transactionDto.getTransactionNumber() + ".error");
            createErrorFileForREST(transactionDto.getTransactionNumber(), errorFile, message, transactionDto);
            moveIfExists(failedCompany, errorFile);

            loggerExporterService.exportWithContentMap(contentMap, message, transactionDto.getTransactionNumber(),
                    logFileInfo, company, false, IMPORT_STATUS_FAILED);
        }

        List<OcmMessage> message = Collections.singletonList(new OcmMessage("Failed when processing transaction"));
        return new OcmTransactionResponse(FAILED, message, transactionDto.getTransactionNumber());
    }

    private boolean isTablesEmpty() {
        // Check if the required tables are empty
        return labelOrderRepository.count() == 0
                || existingBagLatestRepository.count() == 0
                || existingTransactionLatestRepository.count() == 0
                || importerRuleRepository.count() == 0;
    }

    public Transaction saveTransaction(TransactionDto transactionDto, Company company) {
        Transaction convertedTransaction = new Transaction();
        convertedTransaction.setVersion(transactionDto.getVersion());
        convertedTransaction.setDateTime(transactionDto.getDateTime());
        convertedTransaction.setStoreId(transactionDto.getStoreId());
        convertedTransaction.setSerialNumber(transactionDto.getSerialNumber());
        convertedTransaction.setTransactionNumber(transactionDto.getTransactionNumber());
        convertedTransaction.setTotal(transactionDto.getTotal());
        convertedTransaction.setRefundable(transactionDto.getRefundable());
        convertedTransaction.setCollected(transactionDto.getCollected());
        convertedTransaction.setType(FILE_TYPE_TRANSACTION);
        convertedTransaction.setReceivedDate(LocalDateTime.now());
        convertedTransaction.setCharityNumber(transactionDto.getCharityNumber());

        if (ImportedFileValidationHelper.version162Check(transactionDto.getVersion())) {
            if (transactionDto.getNumber() != null && !transactionDto.getNumber().isEmpty()) {
                convertedTransaction.setLabelNumber(transactionDto.getNumber());

                if (transactionDto.getBagType() != null && !transactionDto.getBagType().isEmpty()) {
                    convertedTransaction.setBagType(transactionDto.getBagType());
                } else {
                    convertedTransaction.setBagType("BB");
                }
            }

            convertedTransaction.setManual(transactionDto.getManual());
            convertedTransaction.setRejected(transactionDto.getRejected());
        }

        List<TransactionArticle> transactionArticles = transactionDto.getArticles()
                .stream()
                .map(transactionArticleDto ->
                        conversionService.convert(transactionArticleDto, TransactionArticle.class))
                .collect(Collectors.toList());
        return saveTransactionAndArticlesByCompany(convertedTransaction, transactionArticles, company);
    }

    private OcmTransactionResponse validateTransaction(TransactionDto transactionDto, Company company, String ipAddress, Path accepted) {
        OcmMessage ocmMessage = ValidationUtils.defaultValidation(ipAddress, transactionDto.getVersion(), company);
        //thread safe collection to use in parallel stream bellow
        CopyOnWriteArrayList<OcmMessage> messages = ocmMessage != null ? new CopyOnWriteArrayList<>(List.of(ocmMessage)) : new CopyOnWriteArrayList<>();
        /* if (!CollectionUtils.isEmpty(messages)) {
            return new OcmTransactionResponse(DECLINED, messages, transactionDto.getTransactionNumber());
        } */

        if (Strings.isNullOrEmpty(transactionDto.getTransactionNumber())) {
            messages.add(new OcmMessage("Transaction number " + transactionDto.getTransactionNumber() + " is not valid"));
            return new OcmTransactionResponse(DECLINED, messages, transactionDto.getTransactionNumber());
        }

        if (transactionDto.getTransactionNumber().length() != 21) {
            messages.add(new OcmMessage("Transaction number " + transactionDto.getTransactionNumber() + " needs to be 21 characters long"));
        }
        if (!StringUtils.isNumeric(transactionDto.getTransactionNumber())) {
            messages.add(new OcmMessage("Transaction number" + transactionDto.getTransactionNumber() + " is not numeric"));
        }

        if (Files.exists(accepted.resolve(transactionDto.getTransactionNumber() + "-" + company.getNumber() + ".csv"))) {
            List<OcmMessage> duplicateMessage = Collections.singletonList(new OcmMessage(String.format("Transaction with number %s is already processed and accepted.", transactionDto.getTransactionNumber())));
            return new OcmTransactionResponse(DUPLICATE, duplicateMessage, transactionDto.getTransactionNumber());
        }

        // Check transaction already exists at SRN
        if (existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(transactionDto.getTransactionNumber(), company.getRvmOwnerNumber())) {
            List<OcmMessage> duplicateMessage = Collections.singletonList(new OcmMessage("Transaction is already processed and saved at SNL"));
            return new OcmTransactionResponse(DUPLICATE, duplicateMessage, transactionDto.getTransactionNumber());
        }

        // Check if transaction is unique
        if (transactionRepository.existsByTransactionNumber(transactionDto.getTransactionNumber())) {
            List<OcmMessage> duplicateMessage = Collections.singletonList(new OcmMessage(String.format("Transaction with number %s already exist.", transactionDto.getTransactionNumber())));
            return new OcmTransactionResponse(DUPLICATE, duplicateMessage, transactionDto.getTransactionNumber());
        }

        // Check storeID by IP address
        if (Strings.isNullOrEmpty(transactionDto.getStoreId())) {
            messages.add(new OcmMessage(String.format("RVM Store ID %s is not valid for IP %s.",
                    transactionDto.getStoreId(), ipAddress)));
        } else {
            if (!company.isUsingIpTrunking() && company.getStoreId() != null && !company.getStoreId().equals(transactionDto.getStoreId())) {
                messages.add(new OcmMessage(String.format("RVM Store ID %s is not valid for IP %s.",
                        transactionDto.getStoreId(), ipAddress)));
            } else if (!company.isUsingIpTrunking() && company.getStoreId() == null) {
                messages.add(new OcmMessage(String.format("RVM Store ID %s is not valid for IP %s.",
                        transactionDto.getStoreId(), ipAddress)));
            }
        }

        // Check serialNumber by IP address
        if (Strings.isNullOrEmpty(transactionDto.getSerialNumber())) {
            messages.add(new OcmMessage(String.format("RVM Serial Number %s is not valid for IP %s.",
                    transactionDto.getSerialNumber(), ipAddress)));
        } else {
            if (company.isUsingIpTrunking()) {
                if (!Strings.isNullOrEmpty(transactionDto.getStoreId()) && companyService.existsByStoreIdAndRvmOwnerNumber(transactionDto.getStoreId(), company.getRvmOwnerNumber())) {
                    Company companyByStoreId = companyService.findByStoreIdAndRvmOwnerNumber(transactionDto.getStoreId(), company.getRvmOwnerNumber());
                    if (companyByStoreId.getSerialNumbers().stream().noneMatch(transactionDto.getSerialNumber()::equals)) {
                        messages.add(new OcmMessage(String.format("RVM Serial Number %s is not valid for Store ID %s.",
                                transactionDto.getSerialNumber(), transactionDto.getStoreId())));
                    }
                } else {
                    messages.add(new OcmMessage(String.format("RVM Serial Number %s is not valid for Store ID %s.",
                            transactionDto.getSerialNumber(), transactionDto.getStoreId())));
                }
            } else {
                if (!company.getSerialNumbers().contains(transactionDto.getSerialNumber())) {
                    messages.add(new OcmMessage(String.format("RVM Serial Number %s is not valid for IP %s.",
                            transactionDto.getSerialNumber(), ipAddress)));
                }
            }
        }

        // Check date
        int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
        if (!ValidationUtils.isDateValid(transactionDto.getDateTime(), dataExpirationPeriodInDays)) {
            messages.add(new OcmMessage("Date is not valid."));
        } else {
            LocalDateTime checkDateTime = transactionDto.getDateTime();
            if (transactionRepository.existsByDateTimeAndStoreIdAndSerialNumber(checkDateTime, transactionDto.getStoreId(), transactionDto.getSerialNumber())) {
                messages.add(new OcmMessage(String.format("Transaction with number %s is done at the exact same time as another transaction, this is not possible.", transactionDto.getTransactionNumber())));
            }
        }

        // EAN codes cannot be refunded earlier then activation date with a refund=1 // Now done at other block of code with article handling
        /*if (dtoArticles.stream().anyMatch(a ->
                srnArticleService.existsAndIsActiveByArticleNumberAndRefundable(a.getArticleNumber(), a.getRefund()))) {
            messages.add(new OcmMessage("One of articles activation dates is not valid."));
            return new OcmTransactionResponse(DECLINED, messages, transactionDto.getTransactionNumber());
        }*/

        /*if (transactionDto.getNumber() != null && !transactionDto.getNumber().isEmpty()) {
            if (transactionDto.getBagType() != null && !transactionDto.getBagType().isEmpty() && acceptableBagTypes.stream()
                    .noneMatch(acceptableBagType -> acceptableBagType.equals(transactionDto.getBagType()))) {
                messages.add(new OcmMessage("Bag type does not match acceptable type (BB, SM, CP or MB), check is done because number (label number) and bag type are filled in"));
                return new OcmTransactionResponse(DECLINED, messages, transactionDto.getTransactionNumber());
            }
        }*/

        List<TransactionArticleDto> dtoArticles = transactionDto.getArticles();
        // Check total sums
        if (transactionDto.getRefundable() == null) {
            messages.add(new OcmMessage("Refundable sum is missing"));
        } else if (transactionDto.getRefundable() != dtoArticles.stream().parallel().filter(transactionArticleDto -> transactionArticleDto.getRefund() != null)
                .mapToInt(TransactionArticleDto::getRefund).sum()) {
            messages.add(new OcmMessage("Refundable sum is not equal to refund amount of articles"));
        }
        if (transactionDto.getCollected() == null) {
            messages.add(new OcmMessage("Collected sum is missing"));
        } else if (transactionDto.getCollected() != dtoArticles.stream().parallel().filter(transactionArticleDto -> transactionArticleDto.getCollected() != null)
                .mapToInt(TransactionArticleDto::getCollected).sum()) {
            messages.add(new OcmMessage("Collected sum is not equal to collected amount of articles"));
        }
        if (transactionDto.getTotal() == null) {
            messages.add(new OcmMessage("Total sum is missing"));
        } else if (transactionDto.getTotal() != dtoArticles.size()) {
            messages.add(new OcmMessage("Total sum is not equal to number of articles"));
        }
        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            if (transactionDto.getManual() != null &&
                    transactionDto.getManual() != dtoArticles.stream().parallel().filter(transactionArticleDto -> transactionArticleDto.getManual() != null)
                            .mapToInt(TransactionArticleDto::getManual).sum()) {
                messages.add(new OcmMessage("Manual sum is not equal to manual amount of articles"));
            }
        }

        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            int manualAmount = dtoArticles.stream().parallel().filter(transactionArticleDto -> transactionArticleDto.getManual() != null)
                    .mapToInt(TransactionArticleDto::getManual).sum();
            if (transactionDto.getRejected() != null) {
                if (manualAmount > transactionDto.getRejected()) {
                    messages.add(new OcmMessage("Manual sum amount of articles is " + manualAmount + ", which is greater than the rejected sum amount field value: " + transactionDto.getRejected()));
                }
                if (transactionDto.getRejected() < 0) {
                    messages.add(new OcmMessage("Rejected sum amount is " + transactionDto.getRejected() + ", which is lower than 0 and not possible"));
                }
            }
        }

        if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
            Company companyByStoreId = companyService.findByStoreIdAndRvmOwnerNumber(transactionDto.getStoreId(), company.getRvmOwnerNumber());
            if (!StringUtils.isEmpty(transactionDto.getNumber())) {
                if (!StringUtils.isNumeric(transactionDto.getNumber())) {
                    messages.add(new OcmMessage("Number (label) is not numeric " + transactionDto.getNumber()));
                }
                if (transactionDto.getNumber().length() > 17) {
                    messages.add(new OcmMessage("Number (label) is longer than 17 characters " + transactionDto.getNumber()));
                }
                if (company.getCommunication().equals(REST)) {
                    messages.add(new OcmMessage("Using label number for communication type REST is not possible"));
                }
                if (companyByStoreId != null && companyByStoreId.getType().equals("CUSTOMER")) {
                    messages.add(new OcmMessage("Cannot use a label number when using a store id that is not from a distribution center"));
                }

                // Check transaction bag label already exists at SRN existingBagService
                if (existingBagService.existsByCombinedCustomerNumberLabel(transactionDto.getNumber())) {
                    List<OcmMessage> duplicateMessage = Collections.singletonList(new OcmMessage("Label number " + transactionDto.getNumber() + " is already in use"));
                    return new OcmTransactionResponse(DUPLICATE, duplicateMessage, transactionDto.getTransactionNumber());
                }

                // Check transaction bag label already exists at SRN existingTransactionService
                if (existingTransactionService.existsByCombinedCustomerNumberLabel(transactionDto.getNumber())) {
                    List<OcmMessage> duplicateMessage = Collections.singletonList(new OcmMessage("Label number " + transactionDto.getNumber() + " is already in use"));
                    return new OcmTransactionResponse(DUPLICATE, duplicateMessage, transactionDto.getTransactionNumber());
                }

                // Check if transaction bag label is unique
                if (transactionRepository.existsByLabelNumber(transactionDto.getNumber())) {
                    List<OcmMessage> duplicateMessage = Collections.singletonList(new OcmMessage(String.format("Transaction with label number %s already exist.", transactionDto.getNumber())));
                    return new OcmTransactionResponse(DUPLICATE, duplicateMessage, transactionDto.getTransactionNumber());
                }

                if (transactionDto.getNumber().length() < 7) {
                    messages.add(new OcmMessage("Label number " + transactionDto.getNumber() + " is using the wrong label format"));
                }

                if (transactionDto.getNumber().length() >= 7) {
                    String customerNumberFromLabel = getCustomerNumberFromLabel(transactionDto.getNumber());
                    Company customerByNumber = companyService.findByNumber(customerNumberFromLabel);
                    Integer numberFromLabel = getLabelNumberFromLabel(transactionDto.getNumber());
                    if (customerByNumber != null) {
                        if (customerByNumber.getRvmOwnerNumber() != null) {
                            if (!labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                    customerNumberFromLabel, company.getRvmOwnerNumber(), Long.valueOf(numberFromLabel))) {
                                messages.add(new OcmMessage("Label number " + transactionDto.getNumber() + " is using the wrong label number for the wrong customer number"));
                            }
                        } else {
                            if (!labelOrderService.existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                    customerNumberFromLabel, Long.valueOf(numberFromLabel))) {
                                messages.add(new OcmMessage("Label number " + transactionDto.getNumber() + " is using the wrong label number for the wrong customer number"));
                            }
                        }
                    } else {
                        if (!labelOrderService.existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                                customerNumberFromLabel, Long.valueOf(numberFromLabel))) {
                            messages.add(new OcmMessage("Label number " + transactionDto.getNumber() + " is using the wrong label number for the wrong customer number"));
                        }
                    }
                }

                if (!StringUtils.isEmpty(transactionDto.getBagType())) {
                    if (transactionDto.getBagType().length() != 2) {
                        messages.add(new OcmMessage("Bag type is not 2 characters long"));
                    }
                    if (!ALLOWED_BAG_TYPES.contains(transactionDto.getBagType())) {
                        messages.add(new OcmMessage("Bag type does not match acceptable type (BB, SM, CP or MB), check is done because number (label number) and bag type are filled in"));
                    }
                }
            } else {
                if (companyByStoreId != null && companyByStoreId.getType().equals("DISTRIBUTION_CENTER") && (company.getCommunication().equals(AH_CLOUD) || company.getCommunication().equals(AH_TOMRA))) {
                    messages.add(new OcmMessage("Need to use a label number when using a store id from a distribution center"));
                }
            }
        }

        if (ImportedFileValidationHelper.version17Check(company.getVersion())) {
            if (!StringUtils.isEmpty(transactionDto.getCharityNumber())) {
                if (!companyService.existsByTypeAndNumber(CHARITY_TYPE, transactionDto.getCharityNumber())) {
                    messages.add(new OcmMessage("Charity number " + transactionDto.getCharityNumber() + " does not exist"));
                }
            }
        }

        // Check articles to find if there is one that is passed as not refunded/collected to us but is in our srnArticles as a system bottle that should be refunded.
        dtoArticles.parallelStream().forEach(article -> {
            ImporterRule importerRule = importerRuleService.findByFromEanAndRvmOwnerAndRvmSerial(article.getArticleNumber(), company.getRvmOwnerNumber(), transactionDto.getSerialNumber());
            String articleNumber;
            String articleNumberText;
            if (importerRule != null) {
                articleNumber = importerRule.getToEan();
                articleNumberText = article.getArticleNumber() + " (" + importerRule.getToEan() + ")";
            } else {
                articleNumber = article.getArticleNumber();
                articleNumberText = article.getArticleNumber();
            }

            if (article.getRefund() == null) {
                messages.add(new OcmMessage("Refunded field is missing"));
            } else {
                if (article.getRefund() < 0 || article.getRefund() > 1) {
                    messages.add(new OcmMessage(String.format("Refund field is %s, expected value 0 or 1",
                            article.getRefund())));
                }
            }

            if (article.getCollected() == null) {
                messages.add(new OcmMessage("Collected field is missing"));
            } else {
                if (article.getCollected() < 0 || article.getCollected() > 1) {
                    messages.add(new OcmMessage(String.format("Collected field is %s, expected value 0 or 1",
                            article.getCollected())));
                }
            }

            if (ImportedFileValidationHelper.version15Check(transactionDto.getVersion())) {
                if (article.getMaterial() == null) {
                    messages.add(new OcmMessage("Material field is missing"));
                } else {
                    if (article.getMaterial() < 1 || article.getMaterial() > 4) {
                        messages.add(new OcmMessage(String.format("Material field is %s, expected value between 1 and 4",
                                article.getMaterial())));
                    }
                }
            }
            if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
                if (article.getManual() != null && (article.getManual() < 0 || article.getManual() > 1)) {
                    messages.add(new OcmMessage(String.format("Manual field is %s, expected value 0 or 1",
                            article.getMaterial())));
                }
            }

            SrnArticle srnArticle = srnArticleService.findByArticleNumber(articleNumber);
            if (srnArticle != null) {

                if (article.getRefund() != null && article.getCollected() != null && article.getRefund() == 1 && article.getCollected() == 0) {
                    messages.add(new OcmMessage(String.format("Article with number %s is refunded and should be collected.",
                            articleNumberText)));
                }

                if (article.getRefund() != null && article.getRefund() == 1) {
                    LocalDateTime checkDateTime = transactionDto.getDateTime();
                    if (srnArticle.getFirstArticleActivationDate() == null &&
                            srnArticle.getActivationDate() != null && srnArticle.getActivationDate().isAfter(checkDateTime)) {
                        messages.add(new OcmMessage(String.format("The activation date is in the future for article with number %s",
                                articleNumberText)));
                    }
                    if (srnArticle.getActivationDate() != null &&
                            srnArticle.getFirstArticleActivationDate() != null &&
                            srnArticle.getFirstArticleActivationDate().isBefore(srnArticle.getActivationDate()) &&
                            srnArticle.getFirstArticleActivationDate().isAfter(checkDateTime)) {
                        messages.add(new OcmMessage(String.format("The activation date is in the future for article with number %s",
                                articleNumberText)));
                    }
                }

                double srnArticleMinWeight = srnArticle.getWeight();
                Integer scannedWeight = article.getScannedWeight();
                if (!ImportedFileValidationHelper.version15Check(transactionDto.getVersion()) && scannedWeight == null) {
                    messages.add(new OcmMessage(String.format("Article with number %s scanned weight is absent.",
                            articleNumberText)));
                }

                if (scannedWeight != null && scannedWeight != 0) {
                    if (scannedWeight < srnArticleMinWeight) {
                        messages.add(new OcmMessage(String.format("Article with number %s scanned weight is lower than expected weight.",
                                articleNumberText)));
                    }

                    double srnArticleMaxWeight = srnArticleMinWeight + (srnArticle.getVolume() * 1000 * 0.10); // 10% of volume of article in milligrams.
                    if (scannedWeight > srnArticleMaxWeight) {
                        messages.add(new OcmMessage(String.format("Article with number %s scanned weight is higher than expected weight.",
                                articleNumberText)));
                    }
                }

                if (ImportedFileValidationHelper.version15Check(transactionDto.getVersion())) {
                    if (srnArticle.getMaterial() != null && article.getMaterial() != null && srnArticle.getMaterial().intValue() != article.getMaterial().intValue()) {
                        messages.add(new OcmMessage(String.format("Article with number %s material is different than expected material.",       // todo here
                                articleNumberText)));
                    }
                }
            } else {
                if (article.getRefund() != null && article.getRefund() == 1) {
                    messages.add(new OcmMessage(String.format("Article with number %s does not exist.",
                            articleNumberText)));
                }
            }
        });

        if (!messages.isEmpty()) {
            return new OcmTransactionResponse(DECLINED, messages, transactionDto.getTransactionNumber());
        }

        return new OcmTransactionResponse(ACCEPTED, messages, transactionDto.getTransactionNumber());
    }

    @Override
    public void moveTransactionRestToQueue(TransactionDto transactionDto, Company company) {
        final Path transactionsInQueueRestDir = directoryService.getTransactionsInQueueRestPath();
        if (!FileUtils.checkOrCreateDir(transactionsInQueueRestDir)) {
            log.error("Creating transaction from Rest in queue directory failed");
        }
        Path inQueueCompany = transactionsInQueueRestDir.resolve(company.getIpAddress());
        OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);
        String fileName = transactionDto.getTransactionNumber() + ".json";
        Path file = inQueueCompany.resolve(fileName);
        try {
            String json = objectMapper.writeValueAsString(transactionDto);
            createFile(file, json);
        } catch (IOException e) {
            log.error("Failed to write rest transactions to JSON", e);
            throw new RuntimeException("Transaction rest file not created", e);
        }
        if (Files.exists(inQueueCompany.resolve(fileName))) {
            publisherTransactionImportRest.publishToQueue(
                    new TransactionFilePayloadRest(fileName, company.getId(), FILE_TYPE_TRANSACTION));
            log.info("Published to the transaction rest file import queue {}", file.getFileName());
        }
    }

    private void createFile(Path file, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content);
        }
    }

    @Override
    public Transaction saveTransactionAndArticlesByCompany(Transaction transaction, List<TransactionArticle> transactionArticles, Company company) {
        transaction.setCompanyId(company.getId());
        Transaction savedTransaction = transactionRepository.save(transaction);

        transactionArticles.forEach(transactionArticle -> {
            transactionArticle.setTransactionId(savedTransaction.getId());
            transactionArticleRepository.save(transactionArticle);
        });

        return savedTransaction;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> saveAll(List<Transaction> transactions) {
        return transactionRepository.saveAll(transactions);
    }

    @Override
    public List<TransactionArticle> saveTransactionArticles(List<TransactionArticle> articles) {
        return transactionArticleRepository.saveAll(articles);
    }

    @Override
    public List<Transaction> findByTransactionNumber(String transactionNumber) {
        return transactionRepository.findByTransactionNumber(transactionNumber);
    }

    @Override
    public void delete(Transaction transaction) {
        List<TransactionArticle> transactionArticles = transactionArticleRepository.findAllByTransactionId(transaction.getId());
        transactionArticles.forEach(transactionArticleRepository::delete);
        transactionRepository.delete(transaction);
    }

    @Override
    public void deleteAll(List<Transaction> transactions) {
        transactions.forEach(this::delete);
    }

    @Override
    public List<Transaction> findAllByCompanyId(String companyId) {
        return transactionRepository.findAllByCompanyId(companyId);
    }

    @Override
    public int countAllByTransactionId(String transactionId) {
        return transactionArticleRepository.countAllByTransactionId(transactionId);
    }

    @Override
    public int countAllByTransactionIdAndRefund(String transactionId, Integer refund) {
        return transactionArticleRepository.countAllByTransactionIdAndRefund(transactionId, refund);
    }

    @Override
    public int countAllByTransactionIdAndCollected(String transactionId, Integer collected) {
        return transactionArticleRepository.countAllByTransactionIdAndCollected(transactionId, collected);
    }

    @Override
    public List<TransactionArticle> findAllByTransactionId(String transactionId) {
        return transactionArticleRepository.findAllByTransactionId(transactionId);
    }

    @Override
    public Optional<Transaction> findById(String id) {
        return transactionRepository.findById(id);
    }
}
