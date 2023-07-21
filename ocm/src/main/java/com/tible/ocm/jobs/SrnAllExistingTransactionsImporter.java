package com.tible.ocm.jobs;

import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.ExistingTransactionLatestService;
import com.tible.ocm.services.ExistingTransactionService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;

@Slf4j
@Component
public class SrnAllExistingTransactionsImporter extends AbstractExistingTransactionsImporter {

    private final DirectoryService directoryService;

    public SrnAllExistingTransactionsImporter(BaseTaskService<BaseTask,
            BaseTaskParameter> taskService,
                                              BaseSettingsService<BaseSettings> settingsService,
                                              BaseMailService mailService,
                                              ConsulClient consulClient,
                                              ExistingTransactionService existingTransactionService,
                                              ConversionService conversionService,
                                              ExistingTransactionLatestService existingTransactionLatestService,
                                              DirectoryService directoryService) {
        super(taskService, settingsService, mailService, consulClient, existingTransactionService, conversionService, existingTransactionLatestService);
        this.directoryService = directoryService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getAllExistingTransactionsExportPath())) {
            log.error("Creating all existing transactions directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnAllExistingTransactionsImporter"), key = "task.SrnAllExistingTransactionsImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-all-existing-transactions-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path allExistingTransactionsExportPath = directoryService.getAllExistingTransactionsExportPath();
        processFiles(allExistingTransactionsExportPath, true);
        return true;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnAllExistingTransactionsImporter;
    }
}
