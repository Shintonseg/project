package com.tible.ocm.rabbitmq;

import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.TransactionService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.OcmFileUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ListenerTransactionImportRest}
 */
@ExtendWith(MockitoExtension.class)
class ListenerTransactionImportRestTest {

    private final String companyId = "companyId";
    @InjectMocks
    private ListenerTransactionImportRest listenerTransactionImportRest;

    @Mock
    private TransactionService transactionService;
    @Mock
    private CompanyService companyService;
    @Mock
    private DirectoryService directoryService;
    @Mock
    private MockedStatic<OcmFileUtils> ocmFileUtilsMockedStatic;
    @Mock
    private MockedStatic<Files> filesMockedStatic;
    @Mock
    private MockedStatic<FileUtils> fileUtilsMockedStatic;
    @Mock
    private MockedStatic<ImportHelper> importHelperMockedStatic;

    private TransactionFilePayloadRest payloadRest;
    private Company company;

    @BeforeEach
    void setUp() {
        payloadRest = new TransactionFilePayloadRest();
        payloadRest.setCompanyId(companyId);
        company = new Company();
    }

    @AfterEach
    void tearDown() {
    }

    /**
     * {@link ListenerTransactionImportRest#receiveMessage(TransactionFilePayloadRest)}
     */
    @Test
    void shouldSaveTransaction() {
        // given
        Path transactionsInQueueRestDirMock = mock(Path.class);
        Path inQueueCompanyMock = mock(Path.class);
        Path inQueueRestFilePathMock = mock(Path.class);
        given(companyService.findById(companyId)).willReturn(Optional.ofNullable(company));
        given(directoryService.getTransactionsInQueueRestPath()).willReturn(transactionsInQueueRestDirMock);
        given(transactionsInQueueRestDirMock.resolve(company.getIpAddress())).willReturn(inQueueCompanyMock);
        given(inQueueCompanyMock.resolve(payloadRest.getName())).willReturn(inQueueRestFilePathMock);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompanyMock)).thenReturn(false);
        filesMockedStatic.when(() -> Files.exists(inQueueRestFilePathMock)).thenReturn(true);
        File fileMock = mock(File.class);
        given(inQueueRestFilePathMock.toFile()).willReturn(fileMock);
        fileUtilsMockedStatic.when(() -> FileUtils.readFileToString(fileMock, StandardCharsets.UTF_8)).thenReturn(
                "{}");
        // when
        listenerTransactionImportRest.receiveMessage(payloadRest);
        // then
        then(transactionService).should().saveTransaction(any(TransactionDto.class), eq(company));
    }

    /**
     * {@link ListenerTransactionImportRest#receiveMessage(TransactionFilePayloadRest)}
     */
    @Test
    void shouldMoveFileAndNotSaveTransaction() {
        // given
        Path transactionsInQueueRestDirMock = mock(Path.class);
        Path inQueueCompanyMock = mock(Path.class);
        Path inQueueRestFilePathMock = mock(Path.class);
        given(companyService.findById(companyId)).willReturn(Optional.ofNullable(company));
        given(directoryService.getTransactionsInQueueRestPath()).willReturn(transactionsInQueueRestDirMock);
        given(transactionsInQueueRestDirMock.resolve(company.getIpAddress())).willReturn(inQueueCompanyMock);
        given(inQueueCompanyMock.resolve(payloadRest.getName())).willReturn(inQueueRestFilePathMock);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompanyMock)).thenReturn(false);
        filesMockedStatic.when(() -> Files.exists(inQueueRestFilePathMock)).thenReturn(true);
        File fileMock = mock(File.class);
        given(inQueueRestFilePathMock.toFile()).willReturn(fileMock);
        fileUtilsMockedStatic.when(() -> FileUtils.readFileToString(fileMock, StandardCharsets.UTF_8)).thenThrow(
                IOException.class);
        Path transactionsFailedRestDirMock = mock(Path.class);
        given(directoryService.getTransactionsFailedRestPath()).willReturn(transactionsFailedRestDirMock);
        Path companyFailedPathMock = mock(Path.class);
        given(transactionsFailedRestDirMock.resolve(company.getIpAddress())).willReturn(companyFailedPathMock);
        // when
        listenerTransactionImportRest.receiveMessage(payloadRest);
        // then
        then(transactionService).should(never()).saveTransaction(any(TransactionDto.class), eq(company));
        importHelperMockedStatic.verify(() -> ImportHelper.moveIfExists(companyFailedPathMock, inQueueRestFilePathMock));
    }

    /**
     * {@link ListenerTransactionImportRest#receiveMessage(TransactionFilePayloadRest)}
     */
    @Test
    void shouldNotSaveTransaction() {
        // given
        Path transactionsInQueueRestDirMock = mock(Path.class);
        Path inQueueCompanyMock = mock(Path.class);
        Path inQueueRestFilePathMock = mock(Path.class);
        given(companyService.findById(companyId)).willReturn(Optional.ofNullable(company));
        given(directoryService.getTransactionsInQueueRestPath()).willReturn(transactionsInQueueRestDirMock);
        given(transactionsInQueueRestDirMock.resolve(company.getIpAddress())).willReturn(inQueueCompanyMock);
        given(inQueueCompanyMock.resolve(payloadRest.getName())).willReturn(inQueueRestFilePathMock);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompanyMock)).thenReturn(false);
        filesMockedStatic.when(() -> Files.exists(inQueueRestFilePathMock)).thenReturn(false);
        // when
        listenerTransactionImportRest.receiveMessage(payloadRest);
        // then
        then(transactionService).should(never()).saveTransaction(any(TransactionDto.class), eq(company));
    }
}