package com.tible.ocm.jobs;

import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseEventLogService;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.rabbitmq.PublisherTransactionExport;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.TransactionService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.*;

/**
 * Builds [transactionNumber]-[companyNumber].csv files based on data from MongoDB.
 * Example: 00000000000000022001-010001.csv
 */
@Slf4j
@Component
public class TransactionToFileExporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final TransactionService transactionService;
    private final PublisherTransactionExport publisherTransactionExport;
    private final BaseEventLogService eventLogService;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(REST, AA_TRANSACTION, AA_BAG, SFTP, TOMRA_TRANSACTION, AH_TOMRA, AH_CLOUD);

    @Value("${republish-transaction-before-hours}")
    private Integer republishTransactionBeforeHours;

    public TransactionToFileExporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                     BaseSettingsService<BaseSettings> settingsService,
                                     BaseMailService mailService,
                                     ConsulClient consulClient,
                                     DirectoryService directoryService,
                                     CompanyService companyService,
                                     TransactionService transactionService,
                                     PublisherTransactionExport publisherTransactionExport,
                                     BaseEventLogService eventLogService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.transactionService = transactionService;
        this.publisherTransactionExport = publisherTransactionExport;
        this.eventLogService = eventLogService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsPath())) {
            log.error("Creating transaction directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsAcceptedPath())) {
            log.error("Creating transaction accepted directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getBagsPath())) {
            log.error("Creating AA bag directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getBagsAcceptedPath())) {
            log.error("Creating AA bag accepted directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionToFileExporter"), key = "task.TransactionToFileExporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.transaction-file-exporter}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        /*final Path acceptedPath = directoryService.getTransactionsAcceptedPath();
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(acceptedPath)) {
            log.error("Creating accepted directory failed");
            return false;
        }*/

        List<Company> companies = companyService.findAll().stream()
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> ALLOWED_COMMUNICATION_TYPES.contains(company.getCommunication()))
                .collect(Collectors.toList());

        for (Company company : companies) {
            List<Transaction> transactions = transactionService.findAllByCompanyId(company.getId());
            if (!CollectionUtils.isEmpty(transactions)) {
                transactions.forEach(transaction -> {
                    if (!transaction.getInQueue() && (transaction.getFailed() == null || transaction.getFailed() != null && !transaction.getFailed())) {
                        transaction.setInQueue(true);
                        transaction.setFailed(false);
                        transaction.setInQueueDateTime(LocalDateTime.now());
                        transactionService.save(transaction);
                        publisherTransactionExport.publishToQueue(transaction.getId());
                        log.info("Published transaction id {} to queue for exporting to file", transaction.getId());
                        //eventLogService.createAdd(transaction);
                    } else {
                        if (transaction.getInQueueDateTime() != null && transaction.getInQueueDateTime().isBefore(LocalDateTime.now().minusHours(republishTransactionBeforeHours))) {
                            transaction.setInQueue(true);
                            transaction.setFailed(false);
                            transaction.setInQueueDateTime(LocalDateTime.now());
                            transactionService.save(transaction);
                            publisherTransactionExport.publishToQueue(transaction.getId());
                            log.info("Republished stuck in queue and/or failed transaction id {} to queue for exporting to file", transaction.getId());
                        }
                    }
                });
            }
        }

        return true;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.TransactionToFileExporter;
    }
}
