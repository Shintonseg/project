package com.tible.ocm.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.models.BaseEventLog;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.services.BaseEventLogService;
import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.RejectedTransactionService;
import com.tible.ocm.utils.OcmFileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link RejectedTransactionCleanUpTask}
 */
@ExtendWith(MockitoExtension.class)
class RejectedTransactionCleanUpTaskTests extends BaseJobTest {

    private static final String REJECTED_TRANSACTION_NUMBER = "87654321-8006";
    private static final Path REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR = Path.of("rejectedTransactionsToBeRemoved");
    private static final Path TRANSACTION_FILE_FROM_DISK = Path.of(REJECTED_TRANSACTION_NUMBER + ".csv");
    private static final String FILE_CONTENT = "{\"transactionToBeRemoved\": \"87654321-8006\"}";

    private DirectoryService directoryService;
    private ObjectMapper objectMapper;
    private RejectedTransactionService rejectedTransactionService;
    private BaseEventLogService<BaseEventLog> eventLogService;

    @InjectMocks
    private RejectedTransactionCleanUpTask rejectedTransactionCleanUpTask;

    private MockedStatic<OcmFileUtils> ocmFileUtilsMockedStatic;
    private MockedStatic<Files> filesMockedStatic;

    private RejectedTransaction rejectedTransaction;

    @BeforeEach
    void setUp() {
        /* For constructor and field injection at the same time (CommonTask class)
        https://stackoverflow.com/a/51305235 */
        setUpMocks();
        setUpTask(rejectedTransactionCleanUpTask.getTaskName());

        setUpMockedData();
    }

    @Override
    protected void setUpMocks() {
        super.setUpMocks();
        directoryService = Mockito.mock(DirectoryService.class);
        objectMapper = Mockito.mock(ObjectMapper.class);
        rejectedTransactionService = Mockito.mock(RejectedTransactionService.class);
        eventLogService = Mockito.mock(BaseEventLogService.class);
        ocmFileUtilsMockedStatic = Mockito.mockStatic(OcmFileUtils.class);
        filesMockedStatic = Mockito.mockStatic(Files.class);

        rejectedTransactionCleanUpTask = new RejectedTransactionCleanUpTask(
                taskService, settingsService, baseMailService, consulClient,
                directoryService, objectMapper, rejectedTransactionService, eventLogService
        );
        MockitoAnnotations.openMocks(this);
    }

    private void setUpMockedData() {
        rejectedTransaction = new RejectedTransaction();
        rejectedTransaction.setId("1");
    }

    @AfterEach
    void cleanUp() {
        ocmFileUtilsMockedStatic.close();
        filesMockedStatic.close();
    }

    /**
     * {@link RejectedTransactionCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldDoNothingWhenDirectoryDoesNotExist() {
        //given
        given(directoryService.getRejectedTransactionsToBeRemovedDir())
                .willReturn(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(false);

        //when
        rejectedTransactionCleanUpTask.receiveMessage(taskMessage);

        //then
        then(eventLogService).shouldHaveNoInteractions();
        then(rejectedTransactionService).shouldHaveNoInteractions();
    }

    /**
     * {@link RejectedTransactionCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldDoNothingWhenIOExceptionAndTransactionNumberIsBlank() {
        //given
        given(directoryService.getRejectedTransactionsToBeRemovedDir()).willReturn(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(true);
        filesMockedStatic.when(() -> Files.walk(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(Stream.of(TRANSACTION_FILE_FROM_DISK));
        filesMockedStatic.when(() -> Files.isRegularFile(TRANSACTION_FILE_FROM_DISK)).thenReturn(true);
        filesMockedStatic.when(() -> Files.readAllBytes(TRANSACTION_FILE_FROM_DISK)).thenThrow(IOException.class);

        //when
        rejectedTransactionCleanUpTask.receiveMessage(taskMessage);

        //then
        then(eventLogService).shouldHaveNoInteractions();
        then(rejectedTransactionService).shouldHaveNoInteractions();
    }

    /**
     * {@link RejectedTransactionCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldDeleteFoundTransactionFilesFromDiskAndDeleteFileWithTransactionNumber() throws JsonProcessingException {
        //given
        givenFilesMocks();
        given(directoryService.getRejectedTransactionsToBeRemovedDir())
                .willReturn(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(true);
        filesMockedStatic.when(() -> Files.walk(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(Stream.of(TRANSACTION_FILE_FROM_DISK));
        given(objectMapper.readValue(FILE_CONTENT, Map.class)).willReturn(Map.of("transactionToBeRemoved", REJECTED_TRANSACTION_NUMBER));
        given(directoryService.getRoot()).willReturn(Path.of("root"));
        filesMockedStatic.when(() -> Files.walk(Path.of("root"), Integer.MAX_VALUE))
                .thenReturn(Stream.of(TRANSACTION_FILE_FROM_DISK));

        //when
        rejectedTransactionCleanUpTask.receiveMessage(taskMessage);

        //then
        filesMockedStatic.verify(() -> Files.delete(any()), times(2));
    }

    /**
     * {@link RejectedTransactionCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldRemoveTransactionFromDB() throws JsonProcessingException {
        //given
        String transactionNumber = REJECTED_TRANSACTION_NUMBER.substring(0, REJECTED_TRANSACTION_NUMBER.lastIndexOf("-"));
        givenFilesMocks();

        given(directoryService.getRejectedTransactionsToBeRemovedDir())
                .willReturn(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR);
        ocmFileUtilsMockedStatic.when(() -> OcmFileUtils.checkOrCreateDirWithFullPermissions(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(true);
        filesMockedStatic.when(() -> Files.walk(REJECTED_TRANSACTIONS_TO_BE_REMOVED_DIR))
                .thenReturn(Stream.of(TRANSACTION_FILE_FROM_DISK));
        given(objectMapper.readValue(FILE_CONTENT, Map.class)).willReturn(Map.of("transactionToBeRemoved", REJECTED_TRANSACTION_NUMBER));
        given(directoryService.getRoot()).willReturn(Path.of("root"));
        given(rejectedTransactionService.findAllByBaseFileNameAndType(transactionNumber, RejectedTransaction.TransactionType.TRANSACTION))
                .willReturn(List.of(rejectedTransaction));
        given(rejectedTransactionService.findAllByBaseFileNameAndType(transactionNumber, RejectedTransaction.TransactionType.BAG))
                .willReturn(List.of(rejectedTransaction));

        //when
        rejectedTransactionCleanUpTask.receiveMessage(taskMessage);

        //then
        verify(rejectedTransactionService, times(2)).deleteAll(List.of(rejectedTransaction));
        verify(eventLogService, times(2)).createDelete(List.of(rejectedTransaction));
    }

    private void givenFilesMocks() {
        filesMockedStatic.when(() -> Files.isRegularFile(TRANSACTION_FILE_FROM_DISK)).thenReturn(true);
        filesMockedStatic.when(() -> Files.readAllBytes(TRANSACTION_FILE_FROM_DISK)).thenThrow(IOException.class);
        filesMockedStatic.when(() -> Files.readAllBytes(TRANSACTION_FILE_FROM_DISK)).thenReturn(FILE_CONTENT.getBytes());
    }
}