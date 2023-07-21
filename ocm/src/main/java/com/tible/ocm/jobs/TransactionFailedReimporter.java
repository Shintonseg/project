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
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.services.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.models.CommunicationType.*;

/**
 * Reimports the transactions that are failed in the mongo database.
 */
@Slf4j
@Component
public class TransactionFailedReimporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final TransactionService transactionService;
    private final AAFilesService aaFilesService;
    private final RvmTransactionService rvmTransactionService;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG, SFTP, TOMRA_TRANSACTION, AH_TOMRA);
    private static final List<String> AA_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG);

    public TransactionFailedReimporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                       BaseSettingsService<BaseSettings> settingsService,
                                       BaseMailService mailService,
                                       ConsulClient consulClient,
                                       DirectoryService directoryService,
                                       CompanyService companyService,
                                       TransactionService transactionService,
                                       AAFilesService aaFilesService,
                                       RvmTransactionService rvmTransactionService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.transactionService = transactionService;
        this.aaFilesService = aaFilesService;
        this.rvmTransactionService = rvmTransactionService;
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionFailedReimporter"), key = "task.TransactionFailedReimporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.transaction-failed-reimporter}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        List<Company> companies = companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> ALLOWED_COMMUNICATION_TYPES.contains(company.getCommunication()))
                .collect(Collectors.toList());

        for (Company company : companies) {
            List<Transaction> transactions = transactionService.findAllByCompanyId(company.getId());
            if (!CollectionUtils.isEmpty(transactions)) {
                log.info("Failed transactions found in mongo database for company with ip {}", company.getIpAddress());
                transactions.stream()
                        .filter(transaction -> transaction.getFailed() != null && transaction.getFailed())
                        .forEach(transaction -> {
                    if (AA_COMMUNICATION_TYPES.contains(company.getCommunication())) {
                        try {
                            Path companyBackupPath = company.getCommunication().equals(AA_BAG) ?
                                    directoryService.getBagsBackupPath().resolve(company.getIpAddress()) :
                                    directoryService.getTransactionsBackupPath().resolve(company.getIpAddress());
                            getAABackupFiles(companyBackupPath, transaction.getTransactionNumber(),
                                    readyPath -> aaFilesService.processAABackupOrFailedFiles(company, readyPath, false));
                            log.info("Placed AA transaction id {} back to the company ip folder", transaction.getId());
                            transactionService.delete(transaction);
                        } catch (IOException e) {
                            log.warn("Getting backup files failed", e);
                        }
                    } else {
                        try {
                            Path companyBackupPath = directoryService.getTransactionsBackupPath().resolve(company.getIpAddress());
                            getTransactionBackupFiles(companyBackupPath, transaction.getTransactionNumber(),
                                    csvPath -> rvmTransactionService.processTransactionBackupOrFailedFiles(company, csvPath, false));
                            log.info("Placed transaction id {} back to the company ip folder", transaction.getId());
                            transactionService.delete(transaction);
                        } catch (IOException e) {
                            log.warn("Getting backup files failed", e);
                        }
                    }
                });
            } else {
                log.info("No failed transactions found in mongo database for company with ip {}", company.getIpAddress());
            }
        }
        return true;
    }

    private void getAABackupFiles(Path fromDir, String number, Consumer<Path> consumer) throws IOException {
        try (Stream<Path> paths = Files.find(fromDir, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && (fileName.endsWith(".ready") && fileName.contains(number));
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                consumer.accept(file);
                log.info("Got backup file from {}", file.toString());
            });
        }
    }

    private void getTransactionBackupFiles(Path fromDir, String number, Consumer<Path> consumer) throws IOException {
        try (Stream<Path> paths = Files.find(fromDir, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && (fileName.endsWith(".csv") && fileName.contains(number));
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                consumer.accept(file);
                log.info("Got backup file from {}", file.toString());
            });
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.TransactionFailedReimporter;
    }
}
