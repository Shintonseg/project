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
import com.tible.ocm.services.ExportedTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransactionsCleanupTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final ExportedTransactionService exportedTransactionService;
    @Value("${transactions-cleanup-days}")
    private Integer transactionsCleanupDays;

    public TransactionsCleanupTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                   BaseSettingsService<BaseSettings> settingsService,
                                   BaseMailService mailService,
                                   ConsulClient consulClient,
                                   ExportedTransactionService exportedTransactionService) {
        super(taskService, settingsService, mailService, consulClient);
        this.exportedTransactionService = exportedTransactionService;
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionsCleanupTask"), key = "task.TransactionsCleanupTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.transactions-cleanup-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        exportedTransactionService.deleteByPeriod(transactionsCleanupDays);
        return true;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.TransactionsCleanupTask;
    }
}
