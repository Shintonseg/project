package com.tible.ocm.jobs;

import com.tible.hawk.core.models.BaseTask;
import com.tible.ocm.services.impl.ExistingBagLatestServiceImpl;
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
 * Test for {@link ExistingBagLatestCleanUpTask}
 */
@ExtendWith(MockitoExtension.class)
class ExistingBagLatestCleanUpTaskTest extends BaseJobTest {

    @InjectMocks
    private ExistingBagLatestCleanUpTask existingBagLatestCleanUpTask;
    private ExistingBagLatestServiceImpl existingBagLatestService;

    @BeforeEach
    void setUp() {
        /* For constructor and field injection at the same time (CommonTask class)
        https://stackoverflow.com/a/51305235 */
        setUpMocks();
        setUpTask(existingBagLatestCleanUpTask.getTaskName());

        setUpMockedData();
    }

    @Override
    protected void setUpMocks() {
        super.setUpMocks();
        existingBagLatestService = Mockito.mock(ExistingBagLatestServiceImpl.class);
        existingBagLatestCleanUpTask = new ExistingBagLatestCleanUpTask(
                taskService, settingsService, baseMailService, consulClient, existingBagLatestService
        );
        MockitoAnnotations.openMocks(this);
    }

    private void setUpMockedData() {
        ReflectionTestUtils.setField(existingBagLatestCleanUpTask, "deleteOlderThan", 40);
    }

    /**
     * {@link ExistingBagLatestCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldSetTaskStateToFinishedAfterDeleteByPeriod() {
        //given
        doNothing().when(existingBagLatestService).deleteByPeriod(anyInt());

        //when
        existingBagLatestCleanUpTask.receiveMessage(taskMessage);

        //then
        then(existingBagLatestService).should().deleteByPeriod(anyInt());
        assertEquals(BaseTask.State.FINISHED, baseTask.getState());
    }

    /**
     * {@link ExistingBagLatestCleanUpTask#toExecute(BaseTask)}
     */
    @Test
    void shouldSetTaskStateToFailedWhenExceptionIsThrown() {
        //given
        doThrow(RuntimeException.class).when(existingBagLatestService).deleteByPeriod(anyInt());

        //when
        existingBagLatestCleanUpTask.receiveMessage(taskMessage);

        //then
        then(existingBagLatestService).should().deleteByPeriod(anyInt());
        assertEquals(BaseTask.State.FAILED, baseTask.getState());
    }
}
