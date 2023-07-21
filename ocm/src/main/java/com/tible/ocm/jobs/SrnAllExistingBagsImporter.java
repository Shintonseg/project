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
import com.tible.ocm.services.ExistingBagLatestService;
import com.tible.ocm.services.ExistingBagService;
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
public class SrnAllExistingBagsImporter extends AbstractExistingBagsImporter {

    private final DirectoryService directoryService;

    public SrnAllExistingBagsImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                      BaseSettingsService<BaseSettings> settingsService,
                                      BaseMailService mailService,
                                      ConsulClient consulClient,
                                      DirectoryService directoryService,
                                      ExistingBagService existingBagService,
                                      ExistingBagLatestService existingBagLatestService,
                                      ConversionService conversionService) {
        super(taskService, settingsService, mailService, consulClient, existingBagService, existingBagLatestService, conversionService);
        this.directoryService = directoryService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getAllExistingBagsExportPath())) {
            log.error("Creating all existing bags directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnAllExistingBagsImporter"), key = "task.SrnAllExistingBagsImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-all-existing-bags-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path allExistingBagsExportPath = directoryService.getAllExistingBagsExportPath();
        processFiles(allExistingBagsExportPath, true);
        return true;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnAllExistingBagsImporter;
    }
}
