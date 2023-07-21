package com.tible.ocm.jobs;

import com.tible.hawk.core.models.BaseTask;
import com.tible.ocm.services.impl.ExistingTransactionLatestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * Test for {@link ExistingTransactionLatestCleanUpTask}
 */
@ExtendWith(MockitoExtension.class)
class ExistingTransactionLatestCleanUpTaskTest extends BaseJobTest {

    @InjectMocks
    private ExistingTransactionLatestCleanUpTask existingTransactionLatestCleanUpTask;

    private ExistingTransactionLatestServiceImpl existingTransactionLatestService;

    @BeforeEach
    void setUp() {
        /* For constructor and field injection at the same time (CommonTask class)
        https://stackoverflow.com/a/51305235 */
        setUpMocks();
        setUpTask(existingTransactionLatestCleanUpTask.getTaskName());

        setUpMockedData();
    }

    @Override
    protected void setUpMocks() {
        super.setUpMocks();
        existingTransactionLatestService = Mockito.mock(ExistingTransactionLatestServiceImpl.class);
        existingTransactionLatestCleanUpTask = new ExistingTransactionLatestCleanUpTask(
                taskService, settingsService, baseMailService, consulClient, existingTransactionLatestService
        );
        MockitoAnnotations.openMocks(this);
    }

    private void setUpMockedData() {
        ReflectionTestUtils.setField(existingTransactionLatestCleanUpTask, "deleteOlderThan", 40);
    }

    /**
     * {@link ExistingTransactionLatestCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldSetTaskStateToFinishedAfterDeleteByPeriod() {
        //given
        doNothing().when(existingTransactionLatestService).deleteByPeriod(anyInt());

        //when
        existingTransactionLatestCleanUpTask.receiveMessage(taskMessage);

        //then
        then(existingTransactionLatestService).should().deleteByPeriod(anyInt());
        assertEquals(BaseTask.State.FINISHED, baseTask.getState());
    }

    /**
     * {@link ExistingTransactionLatestCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldSetTaskStateToFailedWhenExceptionIsThrown() {
        //given
        doThrow(RuntimeException.class).when(existingTransactionLatestService).deleteByPeriod(anyInt());

        //when
        existingTransactionLatestCleanUpTask.receiveMessage(taskMessage);

        //then
        then(existingTransactionLatestService).should().deleteByPeriod(anyInt());
        assertEquals(BaseTask.State.FAILED, baseTask.getState());
    }
}
