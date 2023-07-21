package com.tible.ocm.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.ocm.dto.TransactionArticleDto;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.OcmStatus;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.*;
import com.tible.ocm.rabbitmq.PublisherTransactionImportRest;
import com.tible.ocm.rabbitmq.TransactionFilePayloadRest;
import com.tible.ocm.repositories.mongo.*;
import com.tible.ocm.services.*;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import com.tible.ocm.utils.ValidationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

import static com.tible.ocm.services.impl.CompanyServiceImpl.CHARITY_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Test for {@link TransactionServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final Long CLIENT_ID = 1L;
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String PROFILE = "dev";
    private static final String SERIAL_NUMBER = "111";
    private static final Path PATH = Path.of("test");
    private static final String ACCEPTED_STATUS = "ACCEPTED";
    private static final String ALREADY_EXISTS_STATUS = "ALREADY_EXISTS";
    private static final String REJECTED_STATUS = "REJECTED";

    private TransactionServiceImpl transactionService;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionArticleRepository transactionArticleRepository;
    @Mock
    private SrnArticleService srnArticleService;
    @Mock
    private CompanyService companyService;
    @Mock
    private ConversionService conversionService;
    @Mock
    private ExistingTransactionService existingTransactionService;
    @Mock
    private ImporterRuleService importerRuleService;
    @Mock
    private DirectoryService directoryService;
    @Mock
    private LogExporterService<LogFileInfo> loggerExporterService;
    @Mock
    private ExistingBagService existingBagService;
    @Mock
    private LabelOrderService labelOrderService;
    @Mock
    private EnvironmentService environmentService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PublisherTransactionImportRest publisherTransactionImportRest;
    @Mock
    private LabelOrderRepository labelOrderRepository;
    @Mock
    private ExistingBagLatestRepository existingBagLatestRepository;
    @Mock
    private ExistingTransactionLatestRepository existingTransactionLatestRepository;
    @Mock
    private ImporterRuleRepository importerRuleRepository;
    @Mock
    private OAuth2Authentication authentication;
    @Mock
    private OAuth2Request oAuth2Request;
    @Mock
    private HttpServletRequest request;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;
    private MockedStatic<ValidationUtils> validationUtilsMock;
    private MockedStatic<ImportedFileValidationHelper> importedFileValidationHelperMock;
    private MockedStatic<Files> filesMock;
    private MockedStatic<FileUtils> fileUtilsMock;
    private MockedStatic<ImportHelper> importHelperMock;

    @Captor
    private ArgumentCaptor<Transaction> transactionArgCaptor;

    private Company company;
    private Company secondCompany;
    private TransactionDto transactionDto;
    private TransactionDto secondTransactionDto;
    private TransactionDto thirdTransactionDto;
    private SecurityContext securityContext;
    private TransactionArticleDto transactionArticleDto;
    private TransactionArticleDto secondTransactionArticleDto;
    private ImporterRule importerRule;
    private TransactionArticle transactionArticle;
    private Transaction transaction;
    private SrnArticle srnArticle;

    @BeforeEach
    void setUp() {
        setUpMocks();
        setUpMockedData();
    }

    public void setUpMocks() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                transactionArticleRepository,
                srnArticleService,
                companyService,
                conversionService,
                existingTransactionService,
                importerRuleService,
                directoryService,
                loggerExporterService,
                existingBagService,
                labelOrderService,
                environmentService,
                objectMapper,
                publisherTransactionImportRest,
                labelOrderRepository,
                existingBagLatestRepository,
                existingTransactionLatestRepository,
                importerRuleRepository,
                1000);

        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        validationUtilsMock = mockStatic(ValidationUtils.class);
        importedFileValidationHelperMock = mockStatic(ImportedFileValidationHelper.class);
        filesMock = mockStatic(Files.class);
        fileUtilsMock = mockStatic(FileUtils.class);
        importHelperMock = mockStatic(ImportHelper.class);
    }

    public void setUpMockedData() {
        int refundable = 0;
        int collected = 1;

        transactionArticleDto = new TransactionArticleDto();
        transactionArticleDto.setId("1");
        transactionArticleDto.setRefund(refundable);
        transactionArticleDto.setCollected(collected);
        transactionArticleDto.setArticleNumber("111");

        secondTransactionArticleDto = new TransactionArticleDto();
        secondTransactionArticleDto.setId("2");
        secondTransactionArticleDto.setRefund(2);
        secondTransactionArticleDto.setCollected(2);
        secondTransactionArticleDto.setManual(5);
        secondTransactionArticleDto.setMaterial(5);
        secondTransactionArticleDto.setArticleNumber("1");
        secondTransactionArticleDto.setScannedWeight(100000);

        transactionArticle = new TransactionArticle();
        transactionArticle.setId("1");

        company = new Company();
        company.setId("1L");
        company.setNumber("test");
        company.setIpAddress(IP_ADDRESS);
        company.setUsingIpTrunking(false);
        company.setSerialNumbers(List.of(SERIAL_NUMBER));
        company.setStoreId("222");
        company.setRvmOwnerNumber("111");

        secondCompany = new Company();
        secondCompany.setId("2");
        secondCompany.setNumber("2");
        secondCompany.setRvmOwnerNumber("1");
        secondCompany.setUsingIpTrunking(false);
        secondCompany.setIpAddress("1");
        secondCompany.setSerialNumbers(List.of("test"));
        secondCompany.setVersion("1");
        secondCompany.setCommunication("REST");
        secondCompany.setType("CUSTOMER");

        transactionDto = new TransactionDto();
        transactionDto.setId("1");
        transactionDto.setNumber("111");
        transactionDto.setTransactionNumber("123456789123456789123");
        transactionDto.setDateTime(LocalDateTime.now());
        transactionDto.setSerialNumber(SERIAL_NUMBER);
        transactionDto.setStoreId("222");
        transactionDto.setRefundable(refundable);
        transactionDto.setArticles(List.of(transactionArticleDto));
        transactionDto.setCollected(collected);
        transactionDto.setTotal(1);
        transactionDto.setVersion("1");
        transactionDto.setCharityNumber("1");
        transactionDto.setBagType("BB");
        transactionDto.setManual(1);
        transactionDto.setRejected(1);

        secondTransactionDto = new TransactionDto();
        secondTransactionDto.setId("2");
        secondTransactionDto.setTransactionNumber("test");
        secondTransactionDto.setDateTime(LocalDateTime.now());
        secondTransactionDto.setStoreId("1");
        secondTransactionDto.setSerialNumber("1");
        secondTransactionDto.setRefundable(5);
        secondTransactionDto.setRejected(-5);
        secondTransactionDto.setArticles(List.of(secondTransactionArticleDto));
        secondTransactionDto.setCollected(5);
        secondTransactionDto.setTotal(5);
        secondTransactionDto.setManual(5);
        secondTransactionDto.setNumber("esttesttesttesttesttesttestt");

        thirdTransactionDto = new TransactionDto();
        thirdTransactionDto.setId("3");
        thirdTransactionDto.setNumber("11111112222222");
        thirdTransactionDto.setTransactionNumber("123456789123456789123");
        thirdTransactionDto.setRefundable(5);
        thirdTransactionDto.setCollected(5);
        thirdTransactionDto.setTotal(5);
        thirdTransactionDto.setArticles(List.of(secondTransactionArticleDto));
        thirdTransactionDto.setManual(5);
        thirdTransactionDto.setRejected(5);
        thirdTransactionDto.setBagType("B");
        thirdTransactionDto.setCharityNumber("1");
        thirdTransactionDto.setArticles(List.of(secondTransactionArticleDto));
        thirdTransactionDto.setVersion("1");

        transaction = new Transaction();
        transaction.setId("1");

        securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);

        importerRule = new ImporterRule();
        importerRule.setId("1");
        importerRule.setToEan("111");

        srnArticle = new SrnArticle();
        srnArticle.setId("1");
        srnArticle.setWeight(5);
        srnArticle.setVolume(100);
        srnArticle.setMaterial(10);
    }

    @AfterEach
    void cleanUp() {
        securityContextHolderMock.close();
        validationUtilsMock.close();
        importedFileValidationHelperMock.close();
        filesMock.close();
        fileUtilsMock.close();
        importHelperMock.close();
    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDeclinedStatusAndDoesntSaveTransactionWhenCompanyDoesntExist() {
//        //given
//        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
//        given(environmentService.matchGivenProfiles(PROFILE)).willReturn(false);
//        given(companyService.findFirstByIpAddress(IP_ADDRESS)).willReturn(null);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(transactionDto, IP_ADDRESS);
//
//        //then
//        then(environmentService).should().matchGivenProfiles(PROFILE);
//        then(companyService).should().findFirstByIpAddress(IP_ADDRESS);
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        assertEquals(OcmStatus.DECLINED, actual.getStatus());
//        assertEquals(transactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals("Company does not exist with ip " + IP_ADDRESS, actual.getMessages().get(0).getText());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDeclinedStatusAndDoesntSaveTransactionWhenThereAreValidationErrors() {
//        //given
//        givenSaveTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(transactionDto, IP_ADDRESS);
//
//        //then
//        then(environmentService).should().matchGivenProfiles(PROFILE);
//        then(companyService).should().findFirstByIpAddress(CLIENT_ID.toString());
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(company, false, REJECTED_STATUS);
//        assertEquals(OcmStatus.DECLINED, actual.getStatus());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @MockitoSettings(strictness = Strictness.LENIENT)
//    @Test
//    void shouldSaveTransactionAndReturnAcceptedStatusWhenThereAreNoValidationErrors() {
//        //given
//        givenSaveTransaction();
//        givenValidateTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        given(conversionService.convert(transactionArticleDto, TransactionArticle.class)).willReturn(transactionArticle);
//        given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);
//        importedFileValidationHelperMock.when(() -> ImportedFileValidationHelper.version162Check(transactionDto.getVersion()))
//                .thenReturn(true);
//
//        //when
//        transactionService.handleTransaction(transactionDto, IP_ADDRESS);
//
//        //then
//        then(transactionRepository).should().save(any(Transaction.class));
//        thenLoggerExportService(company, false, ACCEPTED_STATUS);
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @MockitoSettings(strictness = Strictness.LENIENT)
//    @Test
//    void shouldSaveTransactionAndReturnAcceptedStatusWhenThereAreNoValidationErrorsAndBagStatusIsNull() {
//        //given
//        transactionDto.setBagType(null);
//        givenSaveTransaction();
//        givenValidateTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        given(conversionService.convert(transactionArticleDto, TransactionArticle.class)).willReturn(transactionArticle);
//        given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);
//        importedFileValidationHelperMock.when(() -> ImportedFileValidationHelper.version162Check(transactionDto.getVersion()))
//                .thenReturn(true);
//
//        //when
//        transactionService.handleTransaction(transactionDto, IP_ADDRESS);
//
//        //then
//        then(transactionRepository).should().save(transactionArgCaptor.capture());
//        thenLoggerExportService(company, false, ACCEPTED_STATUS);
//
//        Transaction actualTransaction = transactionArgCaptor.getValue();
//
//        assertEquals(transactionDto.getVersion(), actualTransaction.getVersion());
//        assertEquals(transactionDto.getDateTime(), actualTransaction.getDateTime());
//        assertEquals(transactionDto.getStoreId(), actualTransaction.getStoreId());
//        assertEquals(transactionDto.getSerialNumber(), actualTransaction.getSerialNumber());
//        assertEquals(transactionDto.getTransactionNumber(), actualTransaction.getTransactionNumber());
//        assertEquals(transactionDto.getTotal(), actualTransaction.getTotal());
//        assertEquals(transactionDto.getRefundable(), actualTransaction.getRefundable());
//        assertEquals(transactionDto.getCollected(), actualTransaction.getCollected());
//        assertEquals(transactionDto.getCharityNumber(), actualTransaction.getCharityNumber());
//        assertEquals("BB", actualTransaction.getBagType());
//        assertEquals("TRANSACTION", actualTransaction.getType());
//        assertEquals(LocalDateTime.now().getMinute(), actualTransaction.getReceivedDate().getMinute());
//    }

    /**
     * * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDuplicateStatusWhenTransactionAlreadyExistsAndProcessed() {
//        //given
//        givenSaveTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        filesMock.when(() -> Files.exists(PATH.resolve(secondTransactionDto.getTransactionNumber() + "-" + company.getNumber() + ".csv")))
//                .thenReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(company, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldMoveDuplicateTransactionToRejectedDirWhenNotifyAboutDoubleTransactionsIsTrue() {
//        //given
//        company.setNotifyAboutDoubleTransactions(true);
//        givenSaveTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        filesMock.when(() -> Files.exists(PATH.resolve(secondTransactionDto.getTransactionNumber() + "-" + company.getNumber() + ".csv")))
//                .thenReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(company, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//        importHelperMock.verify(() ->
//                        ImportHelper.moveIfExists(Path.of("test/test"),
//                                PATH.resolve(IP_ADDRESS).resolve("TRANS/test.error")),
//                times(1));
//    }


    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldMoveDuplicateTransactionToAlreadyExistsDirWhenNotifyAboutDoubleTransactionsIsFalse() {
//        //given
//        company.setNotifyAboutDoubleTransactions(false);
//        givenSaveTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        filesMock.when(() -> Files.exists(PATH.resolve(secondTransactionDto.getTransactionNumber() + "-" + company.getNumber() + ".csv")))
//                .thenReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(company, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//        importHelperMock.verify(() ->
//                        ImportHelper.moveIfExists(Path.of("test"),
//                                PATH.resolve(IP_ADDRESS).resolve("TRANS/test.error")),
//                times(1));
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDuplicateStatusWhenTransactionAlreadyExistsAtSNL() {
//        //given
//        givenSaveTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        given(existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(
//                secondTransactionDto.getTransactionNumber(), company.getRvmOwnerNumber())).willReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(company, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDuplicateStatusWhenTransactionAlreadyExists() {
//        //given
//        givenSaveTransaction();
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(company);
//        given(existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(
//                secondTransactionDto.getTransactionNumber(), company.getRvmOwnerNumber())).willReturn(false);
//        given(transactionRepository.existsByTransactionNumber(secondTransactionDto.getTransactionNumber()))
//                .willReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(company, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDuplicateStatusWhenTransactionAlreadyExistsAtSRNExistingBag() {
//        //given
//        givenSaveTransaction();
//        givenFirstValidationPart();
//        given(companyService.findByStoreIdAndRvmOwnerNumber(
//                secondTransactionDto.getStoreId(), secondCompany.getRvmOwnerNumber())).willReturn(secondCompany);
//        given(existingBagService.existsByCombinedCustomerNumberLabel(secondTransactionDto.getNumber()))
//                .willReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(secondCompany, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDuplicateStatusWhenTransactionAlreadyExistsAtSRNExistingTransaction() {
//        //given
//        givenSaveTransaction();
//        givenFirstValidationPart();
//        given(companyService.findByStoreIdAndRvmOwnerNumber(
//                secondTransactionDto.getStoreId(), secondCompany.getRvmOwnerNumber())).willReturn(secondCompany);
//        given(existingBagService.existsByCombinedCustomerNumberLabel(secondTransactionDto.getNumber()))
//                .willReturn(false);
//        given(existingTransactionService.existsByCombinedCustomerNumberLabel(
//                secondTransactionDto.getNumber()))
//                .willReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(secondCompany, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @Test
//    void shouldReturnDuplicateStatusWhenTransactionLabelIsNotUnique() {
//        //given
//        givenSaveTransaction();
//        givenFirstValidationPart();
//        given(companyService.findByStoreIdAndRvmOwnerNumber(
//                secondTransactionDto.getStoreId(), secondCompany.getRvmOwnerNumber())).willReturn(secondCompany);
//        given(existingBagService.existsByCombinedCustomerNumberLabel(secondTransactionDto.getNumber()))
//                .willReturn(false);
//        given(existingTransactionService.existsByCombinedCustomerNumberLabel(
//                secondTransactionDto.getNumber()))
//                .willReturn(false);
//        given(transactionRepository.existsByLabelNumber(secondTransactionDto.getNumber())).willReturn(true);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(secondTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(secondCompany, true, ALREADY_EXISTS_STATUS);
//        assertEquals(OcmStatus.DUPLICATE, actual.getStatus());
//        assertEquals(secondTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(1, actual.getMessages().size());
//    }

    /**
     * {@link TransactionServiceImpl#handleTransaction(TransactionDto, String)}
     */
//    @MockitoSettings(strictness = Strictness.LENIENT)
//    @Test
//    void shouldReturnValidationErrorsAndDoesntSaveTransaction() {
//        //given
//        int labelNumber = Integer.valueOf(thirdTransactionDto.getNumber().substring(7).trim());
//        int expectedNumberOfErrors = 17;
//        givenSaveTransaction();
//        givenImportedFileValidationHelperMock();
//        validationUtilsMock.when(() -> ValidationUtils.isDateValid(eq(thirdTransactionDto.getDateTime()), anyInt()))
//                .thenReturn(true);
//        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(secondCompany);
//        given(existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(
//                thirdTransactionDto.getTransactionNumber(), secondCompany.getRvmOwnerNumber())).willReturn(false);
//        given(transactionRepository.existsByTransactionNumber(thirdTransactionDto.getTransactionNumber()))
//                .willReturn(false);
//        given(transactionRepository.existsByDateTimeAndStoreIdAndSerialNumber(
//                thirdTransactionDto.getDateTime(),
//                thirdTransactionDto.getStoreId(),
//                thirdTransactionDto.getSerialNumber())).willReturn(true);
//        given(companyService.findByStoreIdAndRvmOwnerNumber(
//                thirdTransactionDto.getStoreId(), secondCompany.getRvmOwnerNumber())).willReturn(secondCompany);
//        given(existingBagService.existsByCombinedCustomerNumberLabel(thirdTransactionDto.getNumber()))
//                .willReturn(false);
//        given(existingTransactionService.existsByCombinedCustomerNumberLabel(
//                thirdTransactionDto.getNumber()))
//                .willReturn(false);
//        given(transactionRepository.existsByLabelNumber(thirdTransactionDto.getNumber())).willReturn(false);
//        given(labelOrderService
//                .existsByCustomerNumberAndLessThanOrEqualFirstLabelNumberAndGreaterThanOrEqualLastLabelNumberAndMarkAllLabelsAsUsedFalse(
//                        eq(null), eq(Long.valueOf(labelNumber)))).willReturn(false);
//        given(companyService.existsByTypeAndNumber(CHARITY_TYPE, thirdTransactionDto.getCharityNumber())).willReturn(true);
//        given(importerRuleService.findByFromEanAndRvmOwnerAndRvmSerial(
//                secondTransactionArticleDto.getArticleNumber(),
//                secondCompany.getRvmOwnerNumber(),
//                thirdTransactionDto.getSerialNumber())).willReturn(importerRule);
//        given(srnArticleService.existsByArticleNumber(importerRule.getToEan())).willReturn(true);
//        given(srnArticleService.findByArticleNumber(importerRule.getToEan())).willReturn(srnArticle);
//        importHelperMock.when(() -> ImportHelper.getLabelNumberFromLabel(thirdTransactionDto.getNumber()))
//                .thenReturn(labelNumber);
//
//        //when
//        OcmTransactionResponse actual = transactionService.handleTransaction(thirdTransactionDto, IP_ADDRESS);
//
//        //then
//        verify(transactionRepository, times(0)).save(any(Transaction.class));
//        thenLoggerExportService(secondCompany, false, REJECTED_STATUS);
//        assertEquals(OcmStatus.DECLINED, actual.getStatus());
//        assertEquals(thirdTransactionDto.getTransactionNumber(), actual.getTransactionNumber());
//        assertEquals(expectedNumberOfErrors, actual.getMessages().size());
//    }

    private void thenLoggerExportService(Company company, boolean alreadyExist, String status) {
        then(loggerExporterService).should().exportWithContentMap(
                anyMap(),
                anyList(),
                anyString(),
                any(LogFileInfo.class),
                eq(company),
                eq(alreadyExist),
                eq(status));
    }

    private void givenSaveTransaction() {
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        given(environmentService.matchGivenProfiles(PROFILE)).willReturn(Boolean.TRUE);
        given(authentication.getOAuth2Request()).willReturn(oAuth2Request);
        given(oAuth2Request.getClientId()).willReturn(CLIENT_ID.toString());
        given(directoryService.getTransactionsRejectedPath()).willReturn(PATH);
        given(directoryService.getRoot()).willReturn(PATH);
        given(directoryService.getTransactionsAlreadyExistsPath()).willReturn(PATH);
        given(directoryService.getTransactionsAcceptedPath()).willReturn(PATH);
        given(directoryService.getTransactionsFailedPath()).willReturn(PATH);
    }

    private void givenFirstValidationPart() {
        given(companyService.findFirstByIpAddress(CLIENT_ID.toString())).willReturn(secondCompany);
        given(existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(
                secondTransactionDto.getTransactionNumber(), secondCompany.getRvmOwnerNumber())).willReturn(false);
        given(transactionRepository.existsByTransactionNumber(secondTransactionDto.getTransactionNumber()))
                .willReturn(false);
        given(transactionRepository.existsByDateTimeAndStoreIdAndSerialNumber(
                secondTransactionDto.getDateTime(),
                secondTransactionDto.getStoreId(),
                secondTransactionDto.getSerialNumber())).willReturn(true);
        validationUtilsMock.when(() -> ValidationUtils.isDateValid(eq(secondTransactionDto.getDateTime()), anyInt()))
                .thenReturn(true);
        importedFileValidationHelperMock.when(() ->
                ImportedFileValidationHelper.version162Check(secondCompany.getVersion())).thenReturn(true);
    }

    private void givenValidateTransaction() {
        validationUtilsMock.when(() -> ValidationUtils.defaultValidation(IP_ADDRESS, transactionDto.getVersion(), company))
                .thenReturn(null);
        validationUtilsMock.when(() -> ValidationUtils.isDateValid(eq(transactionDto.getDateTime()), anyInt()))
                .thenReturn(true);
        given(existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(
                transactionDto.getTransactionNumber(), company.getRvmOwnerNumber())).willReturn(false);
        given(transactionRepository.existsByTransactionNumber(transactionDto.getTransactionNumber()))
                .willReturn(false);
        given(transactionRepository.existsByDateTimeAndStoreIdAndSerialNumber(
                transactionDto.getDateTime(),
                transactionDto.getStoreId(),
                transactionDto.getSerialNumber())).willReturn(false);
        given(importerRuleService.findByFromEanAndRvmOwnerAndRvmSerial(
                transactionArticleDto.getArticleNumber(),
                company.getRvmOwnerNumber(),
                transactionDto.getSerialNumber())).willReturn(importerRule);
        given(srnArticleService.existsByArticleNumber(importerRule.getToEan())).willReturn(false);
    }

    private void givenImportedFileValidationHelperMock() {
        importedFileValidationHelperMock.when(() ->
                ImportedFileValidationHelper.version162Check(secondCompany.getVersion())).thenReturn(true);
        importedFileValidationHelperMock.when(() -> ImportedFileValidationHelper.version17Check(secondCompany.getVersion()))
                .thenReturn(true);
        importedFileValidationHelperMock.when(() -> ImportedFileValidationHelper.version15Check(thirdTransactionDto.getVersion()))
                .thenReturn(true);
    }

    /**
     * {@link TransactionServiceImpl#moveTransactionRestToQueue(TransactionDto, Company)}
     */
    @Test
    void shouldPublishTransactionToQueue() {
        // given
        Path transactionsInQueueRestDirMock = mock(Path.class);
        given(directoryService.getTransactionsInQueueRestPath()).willReturn(transactionsInQueueRestDirMock);
        fileUtilsMock.when(() -> FileUtils.checkOrCreateDir(transactionsInQueueRestDirMock)).thenReturn(false);
        Path inQueueCompanyMock = mock(Path.class);
        Path fileMock = mock(Path.class);
        given(transactionsInQueueRestDirMock.resolve(company.getIpAddress())).willReturn(inQueueCompanyMock);
        filesMock.when(() -> Files.newBufferedWriter(fileMock, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE)).thenReturn(
                mock(BufferedWriter.class));
        filesMock.when(() -> Files.exists(transactionsInQueueRestDirMock)).thenReturn(false);
        given(inQueueCompanyMock.resolve(anyString())).willReturn(fileMock);
        filesMock.when(() -> Files.exists(fileMock)).thenReturn(true);
        // when
        transactionService.moveTransactionRestToQueue(transactionDto, company);
        // then
        then(publisherTransactionImportRest).should().publishToQueue(any(TransactionFilePayloadRest.class));
    }
}
