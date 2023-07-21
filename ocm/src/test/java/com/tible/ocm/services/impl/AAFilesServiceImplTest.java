package com.tible.ocm.services.impl;

import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.helper.AAFiles;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.*;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import com.tible.ocm.utils.OcmFileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Test for {@link AAFilesServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
class AAFilesServiceImplTest {

    private static final String COMPANY_NUMBER = "1111";
    private static final String FILE_NAME_BASE = "937010348000000002101";
    private static final String FILE_NAME_WITH_CUSTOMER_NUMBER = FILE_NAME_BASE + "-" + COMPANY_NUMBER + ".csv";
    private static final String ALREADY_EXISTS_STATUS = "ALREADY_EXISTS";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final Path IP_ADDRESS_PATH = Path.of("192.168.85.7");
    private static final Path READY_PATH = Path.of("transactions/inQueue").resolve(FILE_NAME_BASE + ".csv");
    private static final ImportType IMPORT_TYPE_HDR = new ImportType(1, "HDR");
    private static final ImportType IMPORT_TYPE_POS = new ImportType(2, "POS");

    private AAFilesServiceImpl aaFilesService;

    private DirectoryService directoryService;
    private TransactionService transactionService;
    private ExistingBagService existingBagService;
    private ExistingTransactionService existingTransactionService;
    private LogExporterService<LogFileInfo> loggerExporterService;
    private LabelOrderService labelOrderService;
    private CompanyService companyService;
    private RejectedTransactionService rejectedTransactionService;
    private TransactionRepository transactionRepository;
    private Scanner firstScanner;
    private Scanner secondScanner;

    private MockedStatic<ImportHelper> importHelperMockStatic;
    private MockedStatic<OcmFileUtils> ocmFileMockStatic;
    private MockedStatic<ImportedFileValidationHelper> importedFileValidationHelperMockStatic;
    private MockedStatic<Files> filesMockStatic;
    private MockedStatic<FileUtils> fileUtilsMockStatic;

    @Captor
    private ArgumentCaptor<RejectedTransaction> rejectedTransactionArgCaptor;

    private Company company;
    private AAFiles aaFiles;

    @BeforeEach
    void setUp() {
        setUpMocks();
        setUpMockedData();
    }

    public void setUpMocks() {
        directoryService = mock(DirectoryService.class);
        BaseMailService mailService = mock(BaseMailService.class);
        transactionService = mock(TransactionService.class);
        existingBagService = mock(ExistingBagService.class);
        existingTransactionService = mock(ExistingTransactionService.class);
        loggerExporterService = mock(LogExporterService.class);
        labelOrderService = mock(LabelOrderService.class);
        SrnArticleService srnArticleService = mock(SrnArticleService.class);
        ImporterRuleService importerRuleService = mock(ImporterRuleService.class);
        companyService = mock(CompanyService.class);
        rejectedTransactionService = mock(RejectedTransactionService.class);
        transactionRepository = mock(TransactionRepository.class);
        firstScanner = Mockito.mock(Scanner.class);
        secondScanner = Mockito.mock(Scanner.class);

        aaFilesService = new AAFilesServiceImpl(
                directoryService,
                mailService,
                transactionService,
                existingBagService,
                existingTransactionService,
                loggerExporterService,
                labelOrderService,
                srnArticleService,
                importerRuleService,
                companyService,
                rejectedTransactionService,
                transactionRepository);

        MockitoAnnotations.openMocks(this);

        importHelperMockStatic = mockStatic(ImportHelper.class);
        ocmFileMockStatic = mockStatic(OcmFileUtils.class);
        importedFileValidationHelperMockStatic = mockStatic(ImportedFileValidationHelper.class);
        filesMockStatic = mockStatic(Files.class);
        fileUtilsMockStatic = mockStatic(FileUtils.class);
    }

    public void setUpMockedData() {
        company = new Company();
        company.setId("1");
        company.setNumber(COMPANY_NUMBER);
        company.setIpAddress(IP_ADDRESS_PATH.toString());
        company.setCommunication("AA Transaction");
        company.setRvmOwnerNumber("2222");

        aaFiles = new AAFiles();
        aaFiles.setBatchPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101.batch"));
        aaFiles.setBatchHashPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101_batch.hash"));
        aaFiles.setSlsPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101.sls"));
        aaFiles.setSlsHashPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101_sls.hash"));
        aaFiles.setNlsPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101.nls"));
        aaFiles.setNlsHashPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101_nls.hash"));
        aaFiles.setReadyPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101.ready"));
        aaFiles.setReadyHashPath(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101_ready.hash"));
        aaFiles.setErrorFile(IP_ADDRESS_PATH.resolve("TRANS/937010348000000002101.error"));
    }

    @AfterEach
    void cleanUp() {
        importHelperMockStatic.close();
        ocmFileMockStatic.close();
        importedFileValidationHelperMockStatic.close();
        filesMockStatic.close();
        fileUtilsMockStatic.close();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileWhenNlsFileDoesNotExist() {
        //given
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(any(Path.class))).thenReturn(FILE_NAME_BASE);
        ocmFileMockStatic.when(() -> OcmFileUtils.getAAFiles(READY_PATH, READY_PATH.getParent(), FILE_NAME_BASE))
                .thenReturn(aaFiles);
        filesMockStatic.when(() -> Files.exists(any(Path.class)))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndSaveRejectedTransactionWhenNlsFileDoesNotExist() {
        //given
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(any(Path.class))).thenReturn(FILE_NAME_BASE);
        ocmFileMockStatic.when(() -> OcmFileUtils.getAAFiles(READY_PATH, READY_PATH.getParent(), FILE_NAME_BASE))
                .thenReturn(aaFiles);
        filesMockStatic.when(() -> Files.exists(any(Path.class)))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verify(rejectedTransactionService, times(1)).save(rejectedTransactionArgCaptor.capture());

        RejectedTransaction value = rejectedTransactionArgCaptor.getValue();
        assertEquals(FILE_NAME_BASE, value.getBaseFileName());
        assertEquals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), value.getCreatedAt().truncatedTo(ChronoUnit.MINUTES));
        assertEquals(RejectedTransaction.TransactionType.BAG, value.getType());
        assertEquals(company.getNumber(), value.getCompanyNumber());
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileWhenReadyHashFileDoesNotExist() {
        //given
        givenTransactionDirectories();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(any(Path.class))).thenReturn(FILE_NAME_BASE);
        ocmFileMockStatic.when(() -> OcmFileUtils.getAAFiles(READY_PATH, READY_PATH.getParent(), FILE_NAME_BASE))
                .thenReturn(aaFiles);
        filesMockStatic.when(() -> Files.exists(any(Path.class)))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileWhenHashFileValidationErrorsExist() {
        //given
        givenTransactionDirectories();
        givenFilesExist();
        importHelperMockStatic.when(() -> ImportHelper.getFilename(any(Path.class))).thenReturn(FILE_NAME_BASE);
        ocmFileMockStatic.when(() -> OcmFileUtils.getAAFiles(READY_PATH, READY_PATH.getParent(), FILE_NAME_BASE))
                .thenReturn(aaFiles);
        fileUtilsMockStatic.when(() -> FileUtils.compareSha256HexFromHashFile(any(Path.class), any(Path.class)))
                .thenReturn(false);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToAlreadyExistsDirWhenFileIsAlreadyAcceptedAndNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(true);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToRejectedDirWhenFileIsAlreadyAcceptedAndNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(true);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToAlreadyExistsDirWhenTransactionIsAlreadyExistsAndNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToRejectedDirWhenTransactionIsAlreadyExistsAndNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileWhenCompanyDoesNotHaveMachineFromTransactionNumber() {
        //given
        company.setSerialNumbers(List.of("1"));
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToAlreadyExistsDirWhenTransactionIsAlreadyProcessedAtSnlAndNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(true);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToRejectedDirWhenTransactionIsAlreadyProcessedAtSnlAndNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(true);

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndMoveToFailedDirWhenBatchBodyIsEmpty() {
        //given
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenTransactionDirectories();
        givenFilesExist();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(false);
        importHelperMockStatic.when(() -> ImportHelper.readFileWithLineNumber(any(), any()))
                .thenAnswer(it -> {
                    ((BiConsumer<Scanner, ImportType>) it.getArguments()[1]).accept(firstScanner, IMPORT_TYPE_HDR);
                    return null;
                });
        given(firstScanner.next()).willReturn("test1")
                .willReturn("test2")
                .willReturn("test3")
                .willReturn("20230110")
                .willReturn("test5")
                .willReturn("test6")
                .willReturn("20230111");

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
    }

    /**
     * {@link AAFilesServiceImpl#processAATransactionFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessFileAndSendMailWhenValidationErrors() {
        //given
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenTransactionDirectories();
        givenFilesExist();
        givenHdrScanner();
        givenPosScanner();
        givenReadFileBiConsumer();
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("transactions/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.lazyCheckIsTransactionAlreadyExists(FILE_NAME_BASE, company.getRvmOwnerNumber()))
                .willReturn(false);
        importHelperMockStatic.when(() -> ImportHelper.getLabelOrTransactionNumberFromFileName(FILE_NAME_BASE + ".sls"))
                .thenReturn("12345");
        importHelperMockStatic.when(() -> ImportHelper.getLabelOrTransactionNumberFromFileName(FILE_NAME_BASE + ".nls"))
                .thenReturn("12345");

        //when
        aaFilesService.processAATransactionFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        importHelperMockStatic.verify(() ->
                        ImportHelper.createMail(anyString(), eq(FILE_NAME_BASE), anyList(), eq(null)),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileWhenCustomerDoesNotExist() {
        //given
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(false);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndMoveToAlreadyExistsDirWhenLabelIsAlreadyProcessedAndIsNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndMoveToRejectedDirWhenLabelIsAlreadyProcessedAndIsNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndMoveToAlreadyExistsDirWhenLabelAlreadyExistsAndIsNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.existsByCombinedCustomerNumberLabel(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndMoveToRejectedDirWhenLabelAlreadyExistsAndIsNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.existsByCombinedCustomerNumberLabel(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndMoveToAlreadyExistsDirWhenLabelIsNotUniqueAndIsNotifyAboutDoubleTransactionsIsFalse() {
        //given
        company.setNotifyAboutDoubleTransactions(false);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.existsByCombinedCustomerNumberLabel(FILE_NAME_BASE)).willReturn(false);
        given(transactionRepository.existsByLabelNumber(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToAlreadyExistsDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndMoveToRejectedDirWhenLabelIsNotUniqueAndIsNotifyAboutDoubleTransactionsIsTrue() {
        //given
        company.setNotifyAboutDoubleTransactions(true);
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenSecondFilesChecks();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.existsByCombinedCustomerNumberLabel(FILE_NAME_BASE)).willReturn(false);
        given(transactionRepository.existsByLabelNumber(FILE_NAME_BASE)).willReturn(true);

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        verifyTransactionMoveToRejectedDir();
    }

    /**
     * {@link AAFilesServiceImpl#processAABagFiles(Company, Path, boolean)}
     */
    @Test
    void shouldNotProcessBagFileAndSendMailWhenWrongLabelNumber() {
        //given
        company.setSerialNumbers(List.of(FILE_NAME_BASE.substring(1, 5)));
        givenBagDirectories();
        givenFilesExist();
        givenHdrScanner();
        givenPosScanner();
        givenSecondFilesChecks();
        givenReadFileBiConsumer();
        given(companyService.existsByNumber(company.getNumber())).willReturn(true);
        given(existingBagService.lazyCheckIsBagAlreadyExists(FILE_NAME_BASE)).willReturn(false);
        given(existingTransactionService.existsByCombinedCustomerNumberLabel(FILE_NAME_BASE)).willReturn(false);
        given(transactionRepository.existsByLabelNumber(FILE_NAME_BASE)).willReturn(false);
        given(companyService.findByNumber(company.getNumber())).willReturn(company);
        given(labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
                eq(company.getNumber()), eq(company.getRvmOwnerNumber()), anyLong()))
                .willReturn(false);
        importHelperMockStatic.when(() -> ImportHelper.getLabelOrTransactionNumberFromFileName(FILE_NAME_BASE + ".sls"))
                .thenReturn("12345");
        importHelperMockStatic.when(() -> ImportHelper.getLabelOrTransactionNumberFromFileName(FILE_NAME_BASE + ".nls"))
                .thenReturn("12345");

        //when
        aaFilesService.processAABagFiles(company, READY_PATH, true);

        //then
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        importHelperMockStatic.verify(() ->
                        ImportHelper.createMail(anyString(), eq(FILE_NAME_BASE), anyList(), eq(null)),
                times(1));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }

    private void givenTransactionDirectories() {
        given(directoryService.getTransactionsAcceptedPath()).willReturn(Path.of("transactions/accepted"));
        given(directoryService.getTransactionsRejectedPath()).willReturn(Path.of("transactions/rejected"));
        given(directoryService.getTransactionsFailedPath()).willReturn(Path.of("transactions/failed"));
        given(directoryService.getTransactionsBackupPath()).willReturn(Path.of("transactions/backup"));
        given(directoryService.getTransactionsAlreadyExistsPath()).willReturn(Path.of("transactions/alreadyExists"));

        given(directoryService.getRoot()).willReturn(Path.of("test"));
        given(directoryService.getTransactionLogPath()).willReturn(Path.of("log/transactions"));
    }

    private void givenBagDirectories() {
        given(directoryService.getBagsAcceptedPath()).willReturn(Path.of("bags/accepted"));
        given(directoryService.getBagsRejectedPath()).willReturn(Path.of("bags/rejected"));
        given(directoryService.getBagsFailedPath()).willReturn(Path.of("bags/failed"));
        given(directoryService.getBagsBackupPath()).willReturn(Path.of("bags/backup"));
        given(directoryService.getBagsAlreadyExistsPath()).willReturn(Path.of("bags/alreadyExists"));

        given(directoryService.getRoot()).willReturn(Path.of("test"));
        given(directoryService.getBagLogPath()).willReturn(Path.of("log/bags"));
    }

    private void givenFirstFilesChecks() {
        importHelperMockStatic.when(() -> ImportHelper.getFilename(any(Path.class))).thenReturn(FILE_NAME_BASE);
        ocmFileMockStatic.when(() -> OcmFileUtils.getAAFiles(READY_PATH, READY_PATH.getParent(), FILE_NAME_BASE))
                .thenReturn(aaFiles);
        fileUtilsMockStatic.when(() -> FileUtils.compareSha256HexFromHashFile(any(Path.class), any(Path.class)))
                .thenReturn(true);
        importHelperMockStatic.when(() -> ImportHelper.getLabelOrTransactionNumberFromFileName(READY_PATH))
                .thenReturn(FILE_NAME_BASE);
    }

    private void givenSecondFilesChecks() {
        givenFirstFilesChecks();
        filesMockStatic.when(() -> Files.exists(Path.of("bags/accepted").resolve(FILE_NAME_WITH_CUSTOMER_NUMBER)))
                .thenReturn(false);
        given(transactionRepository.existsByTransactionNumber(FILE_NAME_BASE)).willReturn(false);
        importHelperMockStatic.when(() -> ImportHelper.getCustomerNumberFromLabel(FILE_NAME_BASE))
                .thenReturn(company.getNumber());
    }

    private void givenReadFileBiConsumer() {
        importHelperMockStatic.when(() -> ImportHelper.readFileWithLineNumber(any(), any()))
                .thenAnswer(it -> {
                    ((BiConsumer<Scanner, ImportType>) it.getArguments()[1]).accept(firstScanner, IMPORT_TYPE_HDR);
                    ((BiConsumer<Scanner, ImportType>) it.getArguments()[1]).accept(secondScanner, IMPORT_TYPE_POS);
                    return null;
                });
    }

    private void givenFilesExist() {
        filesMockStatic.when(() -> Files.exists(any(Path.class)))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true);
    }

    private void givenHdrScanner() {
        given(firstScanner.next()).willReturn("test1")
                .willReturn("test2")
                .willReturn("test3")
                .willReturn("20230110")
                .willReturn("test5")
                .willReturn("test6")
                .willReturn("test7")
                .willReturn("20230110150000")
                .willReturn("test8")
                .willReturn("test9")
                .willReturn("20230111")
                .willReturn("test10")
                .willReturn("15:00:00")
                .willReturn("test11")
                .willReturn("test12")    // nls header
                .willReturn("test13")
                .willReturn("test14")
                .willReturn("20230114")
                .willReturn("test16")
                .willReturn("test17")
                .willReturn("test18")
                .willReturn("test19")
                .willReturn("test20");
    }

    private void givenPosScanner() {
        given(secondScanner.next()).willReturn("test11")
                .willReturn("test22")
                .willReturn("test33")
                .willReturn("20230110170000")
                .willReturn("10")
                .willReturn("test55")
                .willReturn("11")
                .willReturn("test77")
                .willReturn("12")
                .willReturn("13")
                .willReturn("test010")
                .willReturn("14")
                .willReturn("test012")
                .willReturn("test013")
                .willReturn("test014")
                .willReturn("18:00:00")
                .willReturn("test015")
                .willReturn("test016")
                .willReturn("20230112")
                .willReturn("test017")
                .willReturn("test018")
                .willReturn("test019")
                .willReturn("test020")
                .willReturn("test021")
                .willReturn("test022")
                .willReturn("test023")    // sls body
                .willReturn("test024")
                .willReturn("test025")
                .willReturn("20230114191010");      // nls body
    }

    private void verifyTransactionMoveToAlreadyExistsDir() {
        then(loggerExporterService).should().exportWithContentMap(
                eq(aaFiles.getReadyPath()), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(true), eq(ALREADY_EXISTS_STATUS));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
    }

    private void verifyTransactionMoveToRejectedDir() {
        importedFileValidationHelperMockStatic.verify(() ->
                        ImportedFileValidationHelper.createErrorFile(eq(FILE_NAME_BASE), eq(aaFiles.getErrorFile()), anyList()),
                times(1));
        then(loggerExporterService).should().exportWithContentMap(
                eq(aaFiles.getReadyPath()), anyMap(), anyList(), any(LogFileInfo.class), eq(company), eq(false), eq(REJECTED_STATUS));
        verify(transactionService, times(0)).saveTransactionAndArticlesByCompany(any(), any(), any());
        verify(rejectedTransactionService, times(1)).save(any());
    }
}