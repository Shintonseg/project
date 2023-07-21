package com.tible.ocm.rabbitmq;

import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.OcmFileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link ListenerTransactionCompanyConfirmed}
 */
@ExtendWith(MockitoExtension.class)
class ListenerTransactionCompanyConfirmedTests {

    private static final String FILE_NAME = "test.confirmed";
    private static final Path TRANSACTIONS_CONFIRMED = Path.of("transactions/confirmed");
    private static final Path BAGS_CONFIRMED = Path.of("bags/confirmed");

    @InjectMocks
    private ListenerTransactionCompanyConfirmed listenerTransactionCompanyConfirmed;

    @Mock
    private DirectoryService directoryService;
    @Mock
    private CompanyService companyService;
    @Mock
    private MockedStatic<OcmFileUtils> ocmFileUtilsMockedStatic;
    @Mock
    private MockedStatic<Files> filesMockedStatic;
    @Mock
    private MockedStatic<ImportHelper> importHelperMockedStatic;

    private TransactionCompanyConfirmedPayload payload;
    private Company company;

    @BeforeEach
    void setUp() {
        setUpMockedData();
    }

    private void setUpMockedData() {
        company = new Company();
        company.setId("1");
        company.setIpAddress("192.168.9.10");

        payload = new TransactionCompanyConfirmedPayload();
        payload.setCompanyId(company.getId());
    }

    /**
     * {@link ListenerTransactionCompanyConfirmed#receiveMessage(TransactionCompanyConfirmedPayload)}
     */
    @Test
    void shouldDoNothingWhenCompanyDoesNotPresent() {
        // given
        given(companyService.findById(company.getId())).willReturn(Optional.empty());

        // when
        listenerTransactionCompanyConfirmed.receiveMessage(payload);

        // then
        then(directoryService).shouldHaveNoInteractions();
        importHelperMockedStatic.verify(
                () -> ImportHelper.copyIfExists(any(), any()),
                times(0)
        );
    }

    /**
     * {@link ListenerTransactionCompanyConfirmed#receiveMessage(TransactionCompanyConfirmedPayload)}
     */
    @Test
    void shouldReturnAndDoesntCopyFilesWhenCreatingTransactionConfirmedCompanyDirectoryFailed() {
        // given
        givenCompanyWithDirectories();
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(any(Path.class)))
                .thenReturn(false);

        // when
        listenerTransactionCompanyConfirmed.receiveMessage(payload);

        // then
        then(directoryService).shouldHaveNoMoreInteractions();
        importHelperMockedStatic.verify(
                () -> ImportHelper.copyIfExists(any(), any()),
                times(0)
        );
    }

    /**
     * {@link ListenerTransactionCompanyConfirmed#receiveMessage(TransactionCompanyConfirmedPayload)}
     */
    @Test
    void shouldReturnAndDoesNotCopyFilesWhenIOException() {
        // given
        givenCompanyWithDirectories();
        given(directoryService.getRoot()).willReturn(Path.of("/RVM"));
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(any(Path.class)))
                .thenReturn(true);
        filesMockedStatic.when(() -> Files.find(any(Path.class), anyInt(), any(), any()))
                .thenThrow(IOException.class);

        // when
        listenerTransactionCompanyConfirmed.receiveMessage(payload);

        // then
        importHelperMockedStatic.verify(
                () -> ImportHelper.getFilename(any()),
                times(0)
        );
        importHelperMockedStatic.verify(
                () -> ImportHelper.copyIfExists(any(), any()),
                times(0)
        );
    }

    /**
     * {@link ListenerTransactionCompanyConfirmed#receiveMessage(TransactionCompanyConfirmedPayload)}
     */
    @Test
    void shouldCopyFiles() {
        // given
        givenCompanyWithDirectories();
        given(directoryService.getRoot()).willReturn(Path.of("/RVM"));
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(any(Path.class)))
                .thenReturn(true);

        filesMockedStatic.when(() -> Files.find(any(Path.class), anyInt(), any(), any()))
                .thenReturn(Stream.of(TRANSACTIONS_CONFIRMED.resolve(FILE_NAME)))
                .thenReturn(Stream.of(BAGS_CONFIRMED.resolve(FILE_NAME)));

        // when
        listenerTransactionCompanyConfirmed.receiveMessage(payload);

        // then
        importHelperMockedStatic.verify(
                () -> ImportHelper.getFilename(any()),
                times(2)
        );
        importHelperMockedStatic.verify(
                () -> ImportHelper.copyIfExists(any(), any()),
                times(2)
        );
    }

    private void givenCompanyWithDirectories() {
        given(companyService.findById(company.getId())).willReturn(Optional.of(company));
        given(directoryService.getTransactionsConfirmedPath()).willReturn(TRANSACTIONS_CONFIRMED);
        given(directoryService.getBagsConfirmedPath()).willReturn(BAGS_CONFIRMED);
    }
}