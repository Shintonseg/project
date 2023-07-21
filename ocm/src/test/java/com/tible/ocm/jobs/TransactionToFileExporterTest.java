package com.tible.ocm.jobs;

import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.services.impl.BaseEventLogServiceImpl;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.rabbitmq.PublisherTransactionExport;
import com.tible.ocm.services.impl.CompanyServiceImpl;
import com.tible.ocm.services.impl.DirectoryServiceImpl;
import com.tible.ocm.services.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Test for {@link TransactionToFileExporter}
 */
@ExtendWith(MockitoExtension.class)
class TransactionToFileExporterTest extends BaseJobTest {

    @InjectMocks
    private TransactionToFileExporter transactionToFileExporter;

    private CompanyServiceImpl companyService;
    private TransactionServiceImpl transactionService;
    private PublisherTransactionExport publisherTransactionExport;

    @Captor
    private ArgumentCaptor<Transaction> transactionArgCaptor;

    private Transaction transaction;
    private Transaction secondTransaction;
    private Company company;

    @BeforeEach
    void setUp() {
        /* For constructor and field injection at the same time (CommonTask class)
        https://stackoverflow.com/a/51305235 */
        setUpMocks();
        setUpTask(transactionToFileExporter.getTaskName());

        setUpMockedData();
    }

    @Override
    protected void setUpMocks() {
        super.setUpMocks();
        DirectoryServiceImpl directoryService = Mockito.mock(DirectoryServiceImpl.class);
        companyService = Mockito.mock(CompanyServiceImpl.class);
        transactionService = Mockito.mock(TransactionServiceImpl.class);
        publisherTransactionExport = Mockito.mock(PublisherTransactionExport.class);
        BaseEventLogServiceImpl eventLogService = Mockito.mock(BaseEventLogServiceImpl.class);
        transactionToFileExporter = new TransactionToFileExporter(
                taskService, settingsService, baseMailService, consulClient,
                directoryService, companyService, transactionService, publisherTransactionExport, eventLogService
        );
        MockitoAnnotations.openMocks(this);
    }

    private void setUpMockedData() {
        ReflectionTestUtils.setField(transactionToFileExporter, "republishTransactionBeforeHours", 10);

        transaction = new Transaction();
        transaction.setId("1");
        transaction.setInQueueDateTime(LocalDateTime.now().minusDays(2));

        company = new Company();
        company.setId("1");
        company.setCommunication("REST");

        secondTransaction = new Transaction();
        secondTransaction.setId("2");
        secondTransaction.setFailed(false);
    }

    /**
     * {@link TransactionToFileExporter#toExecute(BaseTask)}
     */
    @Test
    void shouldSaveTransactionAndSetTaskStateToFinishedWhenTransactionIsNotInQueue() {
        //given
        given(companyService.findAll()).willReturn(List.of(company));
        given(transactionService.findAllByCompanyId(any())).willReturn(List.of(transaction));

        //when
        transactionToFileExporter.receiveMessage(taskMessage);

        //then
        then(transactionService).should().save(transactionArgCaptor.capture());
        assertEquals(BaseTask.State.FINISHED, baseTask.getState());

        Transaction actual = transactionArgCaptor.getValue();

        assertEquals(true, actual.getInQueue());
        assertEquals(false, actual.getFailed());
        assertEquals(LocalDateTime.now().getMinute(), actual.getInQueueDateTime().getMinute());
    }

    /**
     * {@link TransactionToFileExporter#toExecute(BaseTask)}
     */
    @Test
    void shouldSaveTransactionAndSetTaskStateToFinishedWhenTransactionIsInQueueAndDatetimeIsBeforeNow() {
        //given
        transaction.setInQueue(true);
        given(companyService.findAll()).willReturn(List.of(company));
        given(transactionService.findAllByCompanyId(any())).willReturn(List.of(transaction));

        //when
        transactionToFileExporter.receiveMessage(taskMessage);

        //then
        then(transactionService).should().save(transactionArgCaptor.capture());
        then(publisherTransactionExport).should().publishToQueue(transaction.getId());
        assertEquals(BaseTask.State.FINISHED, baseTask.getState());

        Transaction actual = transactionArgCaptor.getValue();

        assertEquals(true, actual.getInQueue());
        assertEquals(false, actual.getFailed());
        assertEquals(LocalDateTime.now().getMinute(), actual.getInQueueDateTime().getMinute());
    }

    /**
     * {@link TransactionToFileExporter#toExecute(BaseTask)}
     */
    @Test
    void shouldSaveTransactionAndSetTaskStateToFinishedWhenTransactionIsNotFailed() {
        //given
        given(companyService.findAll()).willReturn(List.of(company));
        given(transactionService.findAllByCompanyId(any())).willReturn(List.of(secondTransaction));

        //when
        transactionToFileExporter.receiveMessage(taskMessage);

        //then
        then(transactionService).should().save(transactionArgCaptor.capture());
        assertEquals(BaseTask.State.FINISHED, baseTask.getState());

        Transaction actual = transactionArgCaptor.getValue();

        assertEquals(true, actual.getInQueue());
        assertEquals(false, actual.getFailed());
        assertEquals(LocalDateTime.now().getMinute(), actual.getInQueueDateTime().getMinute());
    }
}