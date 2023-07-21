package com.tible.ocm.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.tible.ocm.services.*;
import com.tible.ocm.utils.RejectedFilesUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.*;
import static com.tible.ocm.models.mongo.RejectedTransaction.TransactionType.TRANSACTION;
import static com.tible.ocm.utils.ImportHelper.copyIfExists;
import static com.tible.ocm.utils.ImportHelper.getFilename;

@Slf4j
@Component
public class SynchronizeRejectedTransactionsTask extends AbstractSynchronizeRejectedTask {


    private final DirectoryService directoryService;

    private static final List<String> ALLOWED_TRANSACTION_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION, AH_TOMRA, REST, AH_CLOUD);
    private static final String TRANSACTIONS_REJECTED_DIRECTORIES_SYNCHRONIZED = "transactionsRejectedDirectoriesSynchronized";

    public SynchronizeRejectedTransactionsTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                               BaseSettingsService<BaseSettings> settingsService,
                                               BaseMailService mailService,
                                               ConsulClient consulClient,
                                               CompanyService companyService,
                                               DirectoryService directoryService,
                                               ExistingTransactionService existingTransactionService,
                                               SynchronizedDirectoryService synchronizedDirectoryService,
                                               ObjectMapper objectMapper,
                                               RejectedTransactionService rejectedTransactionService,
                                               @Qualifier("redisClient") RedissonClient redissonClient) {
        super(taskService, settingsService, mailService, consulClient, companyService, directoryService,
                synchronizedDirectoryService, objectMapper, rejectedTransactionService, existingTransactionService,
                redissonClient);
        this.directoryService = directoryService;
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SynchronizeRejectedTransactionsTask"), key = "task.SynchronizeRejectedTransactionsTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.synchronize-rejected-transactions-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path transactionsRejectedPath = directoryService.getTransactionsRejectedPath();
        List<Company> companies = getCompanies(ALLOWED_TRANSACTION_COMMUNICATION_TYPES);

        companies.forEach(company -> {
            Path companyIpRejectedPath = extractRejectedPathFromCompany(company);
            log.info("Remove existing rejected their transaction files from {}", companyIpRejectedPath);
            List<String> existingNumbers = getExistingTransactionNumbers(company);
            removeFoundFiles(companyIpRejectedPath, existingNumbers, TRANSACTION);

            List<String> rejectedTransactions = getRejectedTransactionsFileNames(company);
            removeRejectedTransactionsFiles(companyIpRejectedPath, rejectedTransactions);

            // log.info("Fix owner of transaction files from {}", transactionsRejectedPath.resolve(company.getNumber()));
            fixOwnerOfFiles(transactionsRejectedPath.resolve(company.getNumber()));
        });

        Map<Path, Path> companiesPathMap = companies.stream()
                .collect(Collectors.toMap(key -> transactionsRejectedPath.resolve(key.getNumber()), this::extractRejectedPathFromCompany));
        handleSynchronization(companiesPathMap, TRANSACTIONS_REJECTED_DIRECTORIES_SYNCHRONIZED);

        return true;
    }

    @Override
    protected void handleCustomSynchronization(Path path, Map.Entry<Path, Path> entry) {
        try {
            final Path parent = path.getParent();

            final Path hashFile = parent.resolve(getFilename(path) + ".hash");
            final Path errorFile = parent.resolve(getFilename(path) + ".error");

            Instant errorLastModifiedInstant = Files.readAttributes(errorFile, BasicFileAttributes.class).lastModifiedTime().toInstant();
            Path errorFileIpDir = entry.getValue().resolve(getFilename(path) + ".error");
            boolean errorFileIpExists = Files.exists(errorFileIpDir);
            if (!errorFileIpExists || errorLastModifiedInstant.isAfter(Files.readAttributes(errorFileIpDir, BasicFileAttributes.class).lastModifiedTime().toInstant())) {
                copyIfExists(entry.getValue(), parent, hashFile, errorFile);
                log.info("Synchronized {} rejected transaction files to company ip directory {}", getFilename(path), parent.getParent().getFileName().toString());
            }
        } catch (IOException e) {
            log.warn("Synchronized rejected transaction error: {}", e.getMessage());
        }
    }

    @Override
    protected boolean filterFiles(BasicFileAttributes attributes, String fileName) {
        return !attributes.isDirectory() && fileName.endsWith(".csv");
    }

    @Override
    protected Predicate<Path> deleteRejectedFiles() {
        return RejectedFilesUtils::deleteRejectedTransactionFiles;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SynchronizeRejectedTransactionsTask;
    }
}
