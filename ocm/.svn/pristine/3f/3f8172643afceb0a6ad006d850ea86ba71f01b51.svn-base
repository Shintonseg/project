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
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.rabbitmq.PublisherTransactionCompanyConfirmed;
import com.tible.ocm.rabbitmq.TransactionCompanyConfirmedPayload;
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

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.*;

@Slf4j
@Component
public class ConfirmedFilesExporterTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final CompanyService companyService;
    private final DirectoryService directoryService;
    private final PublisherTransactionCompanyConfirmed publisher;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION,
            AH_TOMRA, AA_BAG, AA_TRANSACTION, REST, AH_CLOUD);

    public ConfirmedFilesExporterTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                      BaseSettingsService<BaseSettings> settingsService,
                                      BaseMailService mailService,
                                      ConsulClient consulClient,
                                      CompanyService companyService,
                                      DirectoryService directoryService,
                                      PublisherTransactionCompanyConfirmed publisher) {
        super(taskService, settingsService, mailService, consulClient);
        this.companyService = companyService;
        this.directoryService = directoryService;
        this.publisher = publisher;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsConfirmedPath())) {
            log.error("Creating transactions confirmed directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getBagsConfirmedPath())) {
            log.error("Creating bags confirmed directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "ConfirmedFilesExporterTask"), key = "task.ConfirmedFilesExporterTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.confirmed-files-exporter-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        List<Company> companies = getCompanies();
        companies.forEach(company -> {
            publishCompanyToQueue(company.getId());
        });

        return true;
    }

    private List<Company> getCompanies() {
        return companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> ALLOWED_COMMUNICATION_TYPES.contains(company.getCommunication()))
                .collect(Collectors.toList());
    }

    private void publishCompanyToQueue(String companyId) {
        TransactionCompanyConfirmedPayload payload = new TransactionCompanyConfirmedPayload();
        payload.setCompanyId(companyId);
        publisher.publishToQueue(payload);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.ConfirmedFilesExporterTask;
    }
}