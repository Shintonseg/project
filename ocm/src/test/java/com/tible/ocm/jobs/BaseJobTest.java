package com.tible.ocm.jobs;

import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.rabbitmq.PublisherTask;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public abstract class BaseJobTest {

    protected BaseTaskService<BaseTask, BaseTaskParameter> taskService;
    protected BaseSettingsService<BaseSettings> settingsService;
    protected BaseMailService baseMailService;
    protected ConsulClient consulClient;

    @Mock
    protected PublisherTask publisherTask;

    @Mock
    protected RedissonClient redissonClient;

    protected TaskMessage taskMessage;
    protected BaseTask baseTask;

    @SuppressWarnings("unchecked")
    protected void setUpMocks() {
        taskService = Mockito.mock(BaseTaskService.class);
        settingsService = Mockito.mock(BaseSettingsService.class);
        baseMailService = Mockito.mock(BaseMailService.class);
        consulClient = Mockito.mock(ConsulClient.class);
    }

    protected void setUpTask(String taskName) {
        taskMessage = new TaskMessage();
        taskMessage.setTaskName(taskName);
        taskMessage.setManual(false);

        var redissonLock = Mockito.mock(RLock.class);
        given(redissonClient.getFairLock(taskName)).willReturn(redissonLock);
        given(redissonLock.tryLock()).willReturn(true);

        baseTask = new BaseTask();
        baseTask.setTaskName(taskName);
        baseTask.setAutomatic(false);
        baseTask.setState(BaseTask.State.TO_BE_STARTED);
        given(taskService.create(any(), any())).willReturn(baseTask);
    }
}
