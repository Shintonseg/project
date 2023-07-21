package com.tible.ocm.services.impl;

import com.tible.hawk.core.utils.FileUtils;
import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.file.TransactionHeader;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.models.mongo.SrnArticle;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.*;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Test for {@link RvmTransactionServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
class RvmTransactionServiceImplTest {

    private static final String COMPANY_NUMBER = "1111";
    private static final String ARTICLE_NUMBER = "2222";
    private static final String FILE_NAME_BASE = "937010348000000002101";
    private static final String RVM_SERIAL = "1234";
    private static final String STORE_ID = "3456";
    private static final int DATA_EXPIRATION_PERIOD_IN_DAYS = 1;
    private static final String REJECTED_STATUS = "REJECTED";
    private static final String ALREADY_EXISTS_STATUS = "ALREADY_EXISTS";
    private static final String ACCEPTED_STATUS = "ACCEPTED";
    private static final String FILE_NAME_WITH_CUSTOMER_NUMBER = FILE_NAME_BASE + "-" + COMPANY_NUMBER + ".csv";
    private static final Path TRANSACTIONS_IN_QUEUE_PATH = Path.of("transactions/inQueue");
    private static final Path IN_QUEUE_CSV_FILE_PATH = TRANSACTIONS_IN_QUEUE_PATH.resolve(FILE_NAME_BASE + ".csv");
    private static final Path IN_QUEUE_HASH_FILE_PATH = TRANSACTIONS_IN_QUEUE_PATH.resolve(FILE_NAME_BASE + ".hash");
    private static final Path IN_QUEUE_ERROR_FILE_PATH = TRANSACTIONS_IN_QUEUE_PATH.resolve(FILE_NAME_BASE + ".error");
    private static final ImportType IMPORT_TYPE_HDR = new ImportType(1, "HDR");
    private static final ImportType IMPORT_TYPE_POS = new ImportType(2, "POS");
    private static final ImportType IMPORT_TYPE_SUM = new ImportType(3, "SUM");

    private RvmTransactionServiceImpl rvmTransactionService;

    private TransactionRepository transactionRepository;
    private DirectoryService directoryService;
    private TransactionService transactionService;
    private SrnArticleService srnArticleService;
    private ExistingTransactionService existingTransactionService;
    private ExistingBagService existingBagService;
    private LabelOrderService labelOrderService;
    private CompanyService companyService;
    private LogExporterService<LogFileInfo> loggerExporterService;
    private RejectedTransactionService rejectedTransactionService;
    private Scanner firstScanner;
    private Scanner secondScanner;
    private Scanner thirdScanner;
    private BufferedWriter writer;

    private MockedStatic<ImportHelper> importHelperMockStatic;
    private MockedStatic<com.tible.hawk.core.utils.ImportHelper> importHelperCoreMockStatic;
    private MockedStatic<Files> filesMockStatic;
    private MockedStatic<FileUtils> fileUtilsMockStatic;
    private MockedStatic<ImportedFileValidationHelper> importedFileValidationHelperMockStatic;
    private MockedStatic<StringUtils> stringUtilsMockStatic;

    @Captor
    private ArgumentCaptor<RejectedTransaction> rejectedTransactionArgCaptor;

    private Company company;
    private SrnArticle article;

    @BeforeEach
    void setUp() {
        setUpMocks();
        setUpMockedData();
    }

    public void setUpMocks() {
        directoryService = mock(DirectoryService.class);
        transactionService = mock(TransactionService.class);
        existingBagService = mock(ExistingBagService.class);
        existingTransactionService = mock(ExistingTransactionService.class);
        transactionRepository = mock(TransactionRepository.class);
        loggerExporterService = mock(LogExporterService.class);
        labelOrderService = mock(LabelOrderService.class);
        srnArticleService = mock(SrnArticleService.class);
        ImporterRuleService importerRuleService = mock(ImporterRuleService.class);
        companyService = mock(CompanyService.class);
        rejectedTransactionService = mock(RejectedTransactionService.class);
        firstScanner = Mockito.mock(Scanner.class);
        secondScanner = Mockito.mock(Scanner.class);
        thirdScanner = Mockito.mock(Scanner.class);
        writer = Mockito.mock(BufferedWriter.class);

        rvmTransactionService = new RvmTransactionServiceImpl(
                transactionRepository,
                directoryService,
                transactionService,
                srnArticleService,
                existingTransactionService,
                existingBagService,
                labelOrderService,
                companyService,
                loggerExporterService,
                importerRuleService,
                rejectedTransactionService);

        MockitoAnnotations.openMocks(this);

        importHelperMockStatic = mockStatic(ImportHelper.class);
        filesMockStatic = mockStatic(Files.class);
        importedFileValidationHelperMockStatic = mockStatic(ImportedFileValidationHelper.class);
        importHelperCoreMockStatic = mockStatic(com.tible.hawk.core.utils.ImportHelper.class);
        fileUtilsMockStatic = mockStatic(FileUtils.class);
        stringUtilsMockStatic = mockStatic(StringUtils.class);
    }

    public void setUpMockedData() {
        company = new Company();
        company.setId("1");
        company.setNumber(COMPANY_NUMBER);
        company.setIpAddress("192.168.85.7");
        company.setCommunication("sFTP");
        company.setRvmOwnerNumber("2222");
        company.setVersion("0162");
        company.setSerialNumbers(List.of(RVM_SERIAL));
        company.setUsingIpTrunking(false);
        company.setStoreId(STORE_ID);

        article = new SrnArticle();
        article.setId("1");
        article.setNumber(ARTICLE_NUMBER);
        article.setWeight(15);
        article.setVolume(5);
    }

    @AfterEach
    void cleanUp() {
        importHelperMockStatic.close();
        filesMockStatic.close();
        importedFileValidationHelperMockStatic.close();
        fileUtilsMockStatic.close();
        importHelperCoreMockStatic.close();
        stringUtilsMockStatic.close();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessTransactionFileAndCreateErrorFileWhenHashFileDoesNotExist() {
        //given
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(IN_QUEUE_CSV_FILE_PATH)).thenReturn(FILE_NAME_BASE);
        filesMockStatic.when(() -> Files.exists(IN_QUEUE_HASH_FILE_PATH)).thenReturn(false);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE + ".csv"), eq(IN_QUEUE_ERROR_FILE_PATH), anyList()),
                times(1));
        verify(rejectedTransactionService, times(1)).save(any());
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(loggerExporterService, times(1))
                .exportWithContentMap(eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(false), eq(REJECTED_STATUS));
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessTransactionFileAndSaveRejectedTransactionWhenHashFileDoesNotExist() {
        //given
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(IN_QUEUE_CSV_FILE_PATH)).thenReturn(FILE_NAME_BASE);
        filesMockStatic.when(() -> Files.exists(IN_QUEUE_HASH_FILE_PATH)).thenReturn(false);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verify(rejectedTransactionService, times(1)).save(rejectedTransactionArgCaptor.capture());

        RejectedTransaction value = rejectedTransactionArgCaptor.getValue();
        assertEquals(FILE_NAME_BASE, value.getBaseFileName());
        assertEquals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), value.getCreatedAt().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RejectedTransaction.TransactionType.TRANSACTION, value.getType());
        assertEquals(company.getNumber(), value.getCompanyNumber());
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToAlreadyExistsDirWhenTransactionIsAlreadyAcceptAndNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(IN_QUEUE_CSV_FILE_PATH)).thenReturn(FILE_NAME_BASE);
        filesMockStatic.when(() -> Files.exists(IN_QUEUE_HASH_FILE_PATH)).thenReturn(true);
        fileUtilsMockStatic.when(() -> FileUtils.compareSha256HexFromHashFile(IN_QUEUE_CSV_FILE_PATH, IN_QUEUE_HASH_FILE_PATH))
                .thenReturn(true);
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToRejectedDirWhenTransactionIsAlreadyAcceptAndNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(IN_QUEUE_CSV_FILE_PATH)).thenReturn(FILE_NAME_BASE);
        filesMockStatic.when(() -> Files.exists(IN_QUEUE_HASH_FILE_PATH)).thenReturn(true);
        fileUtilsMockStatic.when(() -> FileUtils.compareSha256HexFromHashFile(IN_QUEUE_CSV_FILE_PATH, IN_QUEUE_HASH_FILE_PATH))
                .thenReturn(true);
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToAlreadyExistsDirWhenTransactionIsAlreadyInSnlAndNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        givenTransactionDirectories();
        givenInQueueTransactionFiles();
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToRejectedDirWhenTransactionIsAlreadyInSnlAndNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        givenTransactionDirectories();
        givenInQueueTransactionFiles();
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToAlreadyExistsDirWhenTransactionIsNotUniqueAndNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        givenTransactionDirectories();
        givenInQueueTransactionFiles();
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToRejectedDirWhenTransactionIsNotUniqueAndNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        givenTransactionDirectories();
        givenInQueueTransactionFiles();
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldProcessFileAndSaveTransactionWhenThereAreNoValidationErrors() {
        //given
        givenTransactionDirectories();
        givenReadFileBiConsumer();
        givenHeaderScanner();
        givenBodyScanner();
        givenSumScanner();
        givenTransactionFileNameAndCompanyChecks();
        given(directoryService.getTransactionsBackupPath()).willReturn(Path.of("transactions/backup"));
        given(companyService.findByNumber(company.getNumber())).willReturn(company);
        importHelperMockStatic.when(() ->
                        ImportHelper.checkFileContentHeader(
                                eq(company.getNumber()),
                                eq(company.getVersion()),
                                any(TransactionHeader.class),
                                eq(IMPORT_TYPE_HDR),
                                eq(DATA_EXPIRATION_PERIOD_IN_DAYS)))
                .thenReturn(new ArrayList<>());
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("")).thenReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, true);

        //then
        then(transactionService).should().saveTransactionAndArticlesByCompany(any(Transaction.class), anyList(), eq(company));
        importHelperMockStatic.verify(() ->
                        ImportHelper.moveIfExists(eq(Path.of("transactions/backup").resolve(company.getIpAddress())), any(Path.class)),
                times(2));
        then(loggerExporterService).should().exportWithContentMap(
                eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(false), eq(ACCEPTED_STATUS));
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldProcessFileAndDoesNotSaveTransactionWhenThereAreNoValidationErrors() {
        //given
        givenTransactionDirectories();
        givenReadFileBiConsumer();
        givenHeaderScanner();
        givenBodyScanner();
        givenSumScanner();
        givenTransactionFileNameAndCompanyChecks();
        filesMockStatic.when(() -> Files.newBufferedWriter(any(Path.class), eq(StandardOpenOption.TRUNCATE_EXISTING), eq(StandardOpenOption.CREATE)))
                .thenReturn(writer);
        importHelperMockStatic.when(() ->
                        ImportHelper.checkFileContentHeader(
                                eq(company.getNumber()),
                                eq(company.getVersion()),
                                any(TransactionHeader.class),
                                eq(IMPORT_TYPE_HDR),
                                eq(DATA_EXPIRATION_PERIOD_IN_DAYS)))
                .thenReturn(new ArrayList<>());
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("")).thenReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, false);

        //then
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        importHelperMockStatic.verify(() ->
                        ImportHelper.moveAndRenameIfExists(Path.of("transactions/accepted"), IN_QUEUE_CSV_FILE_PATH, FILE_NAME_WITH_CUSTOMER_NUMBER),
                times(1));
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileWhenIOException() {
        //given
        givenTransactionDirectories();
        givenReadFileBiConsumer();
        givenHeaderScanner();
        givenBodyScanner();
        givenSumScanner();
        givenTransactionFileNameAndCompanyChecks();
        filesMockStatic.when(() -> Files.newBufferedWriter(any(Path.class), eq(StandardOpenOption.TRUNCATE_EXISTING), eq(StandardOpenOption.CREATE)))
                .thenThrow(IOException.class);
        importHelperMockStatic.when(() ->
                        ImportHelper.checkFileContentHeader(
                                eq(company.getNumber()),
                                eq(company.getVersion()),
                                any(TransactionHeader.class),
                                eq(IMPORT_TYPE_HDR),
                                eq(DATA_EXPIRATION_PERIOD_IN_DAYS)))
                .thenReturn(new ArrayList<>());
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("")).thenReturn(true);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, false);

        //then
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        then(loggerExporterService).should().exportWithContentMap(
                eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(false), eq("FAILED"));
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE + ".csv"), eq(IN_QUEUE_ERROR_FILE_PATH), anyList()),
                times(1));
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndCreateErrorFileWhenThereAreTransactionHeaderValidationErrors() {
        //given
        List<ImportMessage> importMessages = new ArrayList<>();
        importMessages.add(new ImportMessage(IMPORT_TYPE_HDR.getLineNumber(), "test"));

        givenTransactionDirectories();
        givenReadFileBiConsumer();
        givenHeaderScanner();
        givenBodyScanner();
        givenSumScanner();
        givenTransactionHeaderValidationErrors();
        givenTransactionFileNameAndCompanyChecks();
        importHelperMockStatic.when(() ->
                        ImportHelper.checkFileContentHeader(
                                eq(company.getNumber()),
                                eq(company.getVersion()),
                                any(TransactionHeader.class),
                                eq(IMPORT_TYPE_HDR),
                                eq(DATA_EXPIRATION_PERIOD_IN_DAYS)))
                .thenReturn(importMessages);
        importHelperMockStatic.when(() -> ImportHelper.getCustomerNumberFromLabel(anyString())).thenReturn(COMPANY_NUMBER);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, false);

        //then
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        then(loggerExporterService).should().exportWithContentMap(
                eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(false), eq(REJECTED_STATUS));
        importHelperMockStatic.verify(() -> ImportHelper.createMail(anyString(), eq(FILE_NAME_BASE + ".csv"), anyList(), eq(null)),
                times(1));
    }

    /**
     * {@link RvmTransactionServiceImpl#processTransactionFile(Company, Path, boolean, boolean)}
     */
    @Test
    void shouldNotProcessFileAndCreateErrorFileWhenThereAreTransactionBodyValidationErrors() {
        //given
        List<ImportMessage> importMessages = new ArrayList<>();
        importMessages.add(new ImportMessage(IMPORT_TYPE_HDR.getLineNumber(), "test"));

        givenTransactionDirectories();
        givenReadFileBiConsumer();
        givenHeaderScanner();
        givenBodyScanner();
        givenSumScanner();
        givenTransactionBodyValidationErrors();
        givenTransactionFileNameAndCompanyChecks();
        importHelperMockStatic.when(() ->
                        ImportHelper.checkFileContentHeader(
                                eq(company.getNumber()),
                                eq(company.getVersion()),
                                any(TransactionHeader.class),
                                eq(IMPORT_TYPE_HDR),
                                eq(DATA_EXPIRATION_PERIOD_IN_DAYS)))
                .thenReturn(importMessages);
        importHelperMockStatic.when(() -> ImportHelper.getCustomerNumberFromLabel(anyString())).thenReturn(COMPANY_NUMBER);

        //when
        rvmTransactionService.processTransactionFile(company, IN_QUEUE_CSV_FILE_PATH, true, false);

        //then
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        then(loggerExporterService).should().exportWithContentMap(
                eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(false), eq(REJECTED_STATUS));
        importHelperMockStatic.verify(() -> ImportHelper.createMail(anyString(), eq(FILE_NAME_BASE + ".csv"), anyList(), eq(null)),
                times(1));
    }

    private void givenTransactionDirectories() {
        given(directoryService.getTransactionsRejectedPath()).willReturn(Path.of("transactions/rejected"));
        given(directoryService.getTransactionsFailedPath()).willReturn(Path.of("transactions/failed"));
        given(directoryService.getTransactionsAlreadyExistsPath()).willReturn(Path.of("transactions/alreadyExists"));
        given(directoryService.getTransactionsAcceptedPath()).willReturn(Path.of("transactions/accepted"));

        given(directoryService.getRoot()).willReturn(Path.of("test"));
        given(directoryService.getTransactionLogPath()).willReturn(Path.of("log/transactions"));
    }

    private void givenInQueueTransactionFiles() {
        importHelperMockStatic.when(() -> ImportHelper.getFilename(IN_QUEUE_CSV_FILE_PATH)).thenReturn(FILE_NAME_BASE);
        filesMockStatic.when(() -> Files.exists(IN_QUEUE_HASH_FILE_PATH)).thenReturn(true);
        fileUtilsMockStatic.when(() -> FileUtils.compareSha256HexFromHashFile(IN_QUEUE_CSV_FILE_PATH, IN_QUEUE_HASH_FILE_PATH))
                .thenReturn(true);
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
    }

    private void givenTransactionFileNameAndCompanyChecks() {
        givenInQueueTransactionFiles();
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);
        importedFileValidationHelperMockStatic.when(() -> ImportedFileValidationHelper.version17Check(company.getVersion()))
                .thenReturn(false);
        importedFileValidationHelperMockStatic.when(() -> ImportedFileValidationHelper.version162Check(company.getVersion()))
                .thenReturn(true);
    }

    private void verifyTransactionMoveToAlreadyExistsDir() {
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(loggerExporterService, times(1))
                .exportWithContentMap(eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(true), eq(ALREADY_EXISTS_STATUS));
        importHelperMockStatic.verify(() -> ImportHelper.moveIfExists(Path.of("transactions/alreadyExists"), IN_QUEUE_CSV_FILE_PATH, IN_QUEUE_HASH_FILE_PATH));
    }

    private void verifyTransactionMoveToRejectedDir() {
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(loggerExporterService, times(1))
                .exportWithContentMap(eq(IN_QUEUE_CSV_FILE_PATH), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(true), eq(ALREADY_EXISTS_STATUS));
        importHelperMockStatic.verify(() -> ImportHelper.moveIfExists(Path.of("transactions/rejected/" + COMPANY_NUMBER), IN_QUEUE_CSV_FILE_PATH, IN_QUEUE_HASH_FILE_PATH));
    }

    private void givenReadFileBiConsumer() {
        importHelperCoreMockStatic.when(() -> com.tible.hawk.core.utils.ImportHelper.readFileWithLineNumber(any(), any()))
                .thenAnswer(it -> {
                    ((BiConsumer<Scanner, ImportType>) it.getArguments()[1]).accept(firstScanner, IMPORT_TYPE_HDR);
                    ((BiConsumer<Scanner, ImportType>) it.getArguments()[1]).accept(secondScanner, IMPORT_TYPE_POS);
                    ((BiConsumer<Scanner, ImportType>) it.getArguments()[1]).accept(thirdScanner, IMPORT_TYPE_SUM);
                    return null;
                });
    }

    private void givenHeaderScanner() {
        given(firstScanner.next()).willReturn("test1")  //version
                .willReturn("20230110121212")
                .willReturn(STORE_ID)    // transaction store id
                .willReturn(RVM_SERIAL)     //rvmSerial
                .willReturn("")    //labelNumber
                .willReturn("BBBB");   //bagType

        given(companyService.getDataExpirationPeriodInDays(company)).willReturn(DATA_EXPIRATION_PERIOD_IN_DAYS);
    }

    private void givenBodyScanner() {
        given(secondScanner.next()).willReturn(ARTICLE_NUMBER)    // article number
                .willReturn("10") //scanned weight
                .willReturn("2") //material
                .willReturn("0") //refunded
                .willReturn("1") //collected bottles
                .willReturn("1"); // manual
    }

    private void givenSumScanner() {
        given(thirdScanner.next()).willReturn("3")   //total
                .willReturn("0") // refunded sum
                .willReturn("1")
                .willReturn("1")
                .willReturn("1"); // rejected
    }

    private void givenTransactionHeaderValidationErrors() {
        Integer label = 123;
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty(anyString())).thenReturn(true);
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("")).thenReturn(false);
        stringUtilsMockStatic.when(() -> StringUtils.isNumeric("")).thenReturn(false);
        given(existingTransactionService.existsByCombinedCustomerNumberLabel(anyString())).willReturn(true);
        given(existingBagService.existsByCombinedCustomerNumberLabel(anyString())).willReturn(true);
        given(transactionRepository.existsByLabelNumber(anyString())).willReturn(true);
        given(companyService.findByNumber(COMPANY_NUMBER)).willReturn(company);
        importHelperMockStatic.when(() -> ImportHelper.getCustomerNumberFromLabel(anyString())).thenReturn(COMPANY_NUMBER);
        importHelperMockStatic.when(() -> ImportHelper.getLabelNumberFromLabel(anyString())).thenReturn(label);
        given(labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(any(), any(), any()))
                .willReturn(false);
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("BBBB")).thenReturn(false);
    }

    private void givenTransactionBodyValidationErrors() {
        given(srnArticleService.getAll()).willReturn(List.of(article));
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("10")).thenReturn(false);  //scanned weight
        stringUtilsMockStatic.when(() -> StringUtils.isEmpty("1")).thenReturn(true);      //collected
    }
}
