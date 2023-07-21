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
import com.tible.hawk.core.utils.FileUtils;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.rabbitmq.PublisherTransactionImport;
import com.tible.ocm.rabbitmq.PublisherTransactionImportBigFiles;
import com.tible.ocm.rabbitmq.TransactionFilePayload;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.utils.ImportHelper;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.models.CommunicationType.*;
import static com.tible.ocm.utils.ImportHelper.*;

/**
 * It will go over all the company ip address folders and import their transaction files.
 */
@Component
@Slf4j
public class TransactionPerCompanyFileImporter extends CommonTask<BaseTask, BaseTaskParameter> {
    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final PublisherTransactionImport publisherTransactionImport;
    private final PublisherTransactionImportBigFiles publisherTransactionImportBigFiles;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION, AH_TOMRA, REST, AH_CLOUD);

    public TransactionPerCompanyFileImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                             BaseSettingsService<BaseSettings> settingsService,
                                             BaseMailService mailService,
                                             ConsulClient consulClient,
                                             DirectoryService directoryService,
                                             CompanyService companyService,
                                             PublisherTransactionImport publisherTransactionImport,
                                             PublisherTransactionImportBigFiles publisherTransactionImportBigFiles) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.publisherTransactionImport = publisherTransactionImport;
        this.publisherTransactionImportBigFiles = publisherTransactionImportBigFiles;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsPath())) {
            log.error("Creating transaction directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionPerCompanyFileImporter"), key = "task.TransactionPerCompanyFileImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.transaction-per-company-file-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        /*if (checkMount && !isMounted(directoryService.getRoot().toString())) {
            LOGGER.error("Directory {} is not mounted", transactionsFromDir.toString());
            return false;
        }*/

        final Path transactionsInQueueDir = directoryService.getTransactionsInQueuePath();
        if (!FileUtils.checkOrCreateDir(transactionsInQueueDir)) {
            log.error("Creating transaction in queue directory failed");
        }

        final Path transactionsInQueueBigFilesDir = directoryService.getTransactionsInQueueBigFilesPath();
        if (!FileUtils.checkOrCreateDir(transactionsInQueueBigFilesDir)) {
            log.error("Creating transaction in queue (Big files) directory failed");
        }

        List<Company> companies = companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> ALLOWED_COMMUNICATION_TYPES.contains(company.getCommunication()))
                .collect(Collectors.toList());

        companies.forEach(company -> {
            try {
                Path companyPath = directoryService.getRoot().resolve(company.getIpAddress());
                Path companyTransPath = companyPath.resolve(TRANS_DIRECTORY);
                if (Files.exists(companyPath) && Files.exists(companyTransPath)) {
                    Path inQueueCompany = transactionsInQueueDir.resolve(company.getIpAddress());
                    OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);

                    Path inQueueBigFilesCompany = transactionsInQueueBigFilesDir.resolve(company.getIpAddress());
                    OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueBigFilesCompany);

                    placeInQueueTransactionsFiles(company, companyTransPath, inQueueCompany, inQueueBigFilesCompany);
                }
            } catch (IOException e) {
                log.warn("Processing files failed", e);
            }
        });

        return true;
    }

    private void placeInQueueTransactionsFiles(Company company, Path transactionsFromDir,
                                               Path inQueueCompany, Path inQueueBigFilesCompany) throws IOException {
        try (Stream<Path> paths = Files.find(transactionsFromDir, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".csv");
        })) {
            // Collecting necessary, otherwise it goes wrong on linux (caching it seems)
            paths.collect(Collectors.toList()).forEach(file -> {
                try {
                    if (Files.size(file) / 1024 > 50) {
                        getAndMoveFilesToQueue(file, company, inQueueBigFilesCompany, true);
                    } else {
                        getAndMoveFilesToQueue(file, company, inQueueCompany, false);
                    }
                } catch (IOException e) {
                    log.warn("Processing file {} size failed", file.getFileName(), e);
                }
            });
        }
    }

    private void getAndMoveFilesToQueue(Path file, Company company, Path inQueueCompany, boolean bigFile) {
        final Path parent = file.getParent();
        final Path hashFile = parent.resolve(getFilename(file) + ".hash");
        moveFilesToQueue(inQueueCompany, file, hashFile);

        if (Files.exists(inQueueCompany.resolve(file.getFileName().toString()))) {
            if (bigFile) {
                publisherTransactionImportBigFiles.publishToQueue(new TransactionFilePayload(file.getFileName().toString(),
                        company.getId(), ImportHelper.FILE_TYPE_TRANSACTION));
                log.info("Published to the transaction/bag file import (Big files) queue {}", file.getFileName());
            } else {
                publisherTransactionImport.publishToQueue(new TransactionFilePayload(file.getFileName().toString(),
                        company.getId(), ImportHelper.FILE_TYPE_TRANSACTION));
                log.info("Published to the transaction/bag file import queue {}", file.getFileName());
            }
        }
    }

    private void moveFilesToQueue(Path inQueueDir, Path file, Path hashFile) {
        moveIfExists(inQueueDir, file);
        moveIfExists(inQueueDir, hashFile);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.TransactionPerCompanyFileImporter;
    }
}
