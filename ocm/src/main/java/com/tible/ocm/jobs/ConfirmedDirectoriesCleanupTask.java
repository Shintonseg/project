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
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

import static com.tible.ocm.utils.ImportHelper.*;

@Slf4j
@Component
public class ConfirmedDirectoriesCleanupTask extends AbstractDirectoriesCleanupTask {

    public ConfirmedDirectoriesCleanupTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                           BaseSettingsService<BaseSettings> settingsService,
                                           BaseMailService mailService,
                                           ConsulClient consulClient,
                                           DirectoryService directoryService,
                                           CompanyService companyService) {
        super(taskService, settingsService, mailService, consulClient, directoryService, companyService);
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "ConfirmedDirectoriesCleanupTask"), key = "task.ConfirmedDirectoriesCleanupTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.confirmed-files-cleanup-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .forEach(company -> {
                    Path path = extractConfirmedPathFromCompany(company);
                    cleanupExpiredFilesFromDirectory(path, company);
                });
        final Path transactionsConfirmedDir = directoryService.getTransactionsConfirmedPath();
        final Path bagsConfirmedDir = directoryService.getBagsConfirmedPath();
        if (OcmFileUtils.checkOrCreateDirWithFullPermissions(transactionsConfirmedDir)) {
            cleanupExpiredFilesFromIpDirectory(transactionsConfirmedDir);
        }
        if (OcmFileUtils.checkOrCreateDirWithFullPermissions(bagsConfirmedDir)) {
            cleanupExpiredFilesFromIpDirectory(bagsConfirmedDir);
        }
        return true;
    }

    private Path extractConfirmedPathFromCompany(Company company) {
        Path companyPath = directoryService.getRoot().resolve(company.getIpAddress());
        return companyPath.resolve(TRANS_DIRECTORY)
                .resolve(CONFIRMED_DIRECTORY);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.ConfirmedDirectoriesCleanupTask;
    }
}
