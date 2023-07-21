package com.tible.ocm.jobs;

import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.services.ExistingBagLatestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExistingBagLatestCleanUpTask extends CommonTask<BaseTask, BaseTaskParameter> {
    private final ExistingBagLatestService existingBagLatestService;
    @Value("${existing-bag-latest-cleanup-days}")
    private Integer deleteOlderThan;

    public ExistingBagLatestCleanUpTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                        BaseSettingsService<BaseSettings> settingsService,
                                        BaseMailService mailService,
                                        ConsulClient consulClient,
                                        ExistingBagLatestService existingBagLatestService) {
        super(taskService, settingsService, mailService, consulClient);
        this.existingBagLatestService = existingBagLatestService;
    }

    @Override
    @RabbitListener(bindings = {@QueueBinding(
            value = @Queue(value = "ExistingBagLatestCleanUpTask"),
            key = "task.ExistingBagLatestCleanUpTask",
            exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    protected void receiveMessage(TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.existing-bag-latest-cleanup-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(BaseTask task) {
        try {
            existingBagLatestService.deleteByPeriod(deleteOlderThan);
        } catch (Exception e) {
            log.error("Error occurred while try to clean up ExistingBagLatest", e);
            return false;
        }
        return true;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.ExistingBagLatestCleanUpTask;
    }
}
