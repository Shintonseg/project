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
import com.tible.ocm.configurations.DefaultClientDirectoriesConfiguration;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.AA_BAG;
import static com.tible.ocm.models.CommunicationType.AA_TRANSACTION;

@Slf4j
@Component
public class DefaultClientDirectoriesCreationTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final CompanyService companyService;
    private final DirectoryService directoryService;
    private final DefaultClientDirectoriesConfiguration clientDirectories;

    private static final List<String> AA_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG);

    public DefaultClientDirectoriesCreationTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                                BaseSettingsService<BaseSettings> settingsService,
                                                BaseMailService mailService,
                                                ConsulClient consulClient,
                                                CompanyService companyService,
                                                DirectoryService directoryService,
                                                DefaultClientDirectoriesConfiguration clientDirectories) {
        super(taskService, settingsService, mailService, consulClient);
        this.companyService = companyService;
        this.directoryService = directoryService;
        this.clientDirectories = clientDirectories;
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "DefaultClientDirectoriesCreationTask"), key = "task.DefaultClientDirectoriesCreationTask",
                    exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.default-client-directories-creation-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path transactionsRejectedPath = directoryService.getTransactionsRejectedPath();
        List<Company> companies = companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .collect(Collectors.toList());

        companies.forEach(company -> {
            Path companyIpPath = directoryService.getRoot().resolve(company.getIpAddress());

            if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(companyIpPath)) {
                log.error("Creating client directory {} failed", companyIpPath);
            }

            clientDirectories.getAllInnerDirectories().forEach(dir -> {
                Path innerDir = companyIpPath.resolve(dir);
                Path rejectedDir = innerDir.resolve(clientDirectories.getRejectedDirectory());
                if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(innerDir) || !OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedDir)) {
                    log.error("Creating client inner directories {} and {} failed", innerDir, rejectedDir);
                }
            });

            // createAACompanyDefaultDirectories(company, companyPath); Not needed anymore, because an company can only deliver label or transaction based files for AA. Not both of them.

            Path companyNumberPath = transactionsRejectedPath.resolve(company.getNumber());
            if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(companyNumberPath)) {
                log.error("Creating rejected company number transactions directory {} failed", companyNumberPath);
            }
        });

        return true;
    }

    @Deprecated
    private void createAACompanyDefaultDirectories(Company company, Path companyPath) {
        if (company.getCommunication() != null && AA_COMMUNICATION_TYPES.contains(company.getCommunication())) {
            Path innerDir = companyPath.resolve(clientDirectories.getBagsDirectory());
            Path rejectedDir = innerDir.resolve(clientDirectories.getRejectedDirectory());
            if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(innerDir) || !OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedDir)) {
                log.error("Creating client inner directories {} and {} failed", innerDir, rejectedDir);
            }
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.DefaultClientDirectoriesCreationTask;
    }

}
