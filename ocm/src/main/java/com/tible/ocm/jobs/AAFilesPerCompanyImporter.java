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
import com.tible.ocm.dto.helper.AAFiles;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.rabbitmq.PublisherTransactionImport;
import com.tible.ocm.rabbitmq.PublisherTransactionImportBigFiles;
import com.tible.ocm.rabbitmq.TransactionFilePayload;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.models.CommunicationType.AA_BAG;
import static com.tible.ocm.models.CommunicationType.AA_TRANSACTION;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.OcmFileUtils.getAAFiles;

@Component
@Slf4j
public class AAFilesPerCompanyImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final PublisherTransactionImport publisherTransactionImport;
    private final PublisherTransactionImportBigFiles publisherTransactionImportBigFiles;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG);

    public AAFilesPerCompanyImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
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
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getBagsPath())) {
            log.error("Creating bag directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsPath())) {
            log.error("Creating transaction directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "AAFilesPerCompanyImporter"), key = "task.AAFilesPerCompanyImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Override
    @Scheduled(cron = "${tasks.aa-files-per-company-importer}")
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path bagsInQueueDir = directoryService.getBagsInQueuePath();
        if (!FileUtils.checkOrCreateDir(bagsInQueueDir)) {
            log.error("Creating bags in queue directory failed");
        }
        final Path bagsInQueueBigFilesDir = directoryService.getBagsInQueueBigFilesPath();
        if (!FileUtils.checkOrCreateDir(bagsInQueueBigFilesDir)) {
            log.error("Creating bags in queue (Big files) directory failed");
        }


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
                    if (company.getCommunication().equals(AA_TRANSACTION)) {
                        Path inQueueCompany = transactionsInQueueDir.resolve(company.getIpAddress());
                        OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);

                        Path inQueueBigFilesCompany = transactionsInQueueBigFilesDir.resolve(company.getIpAddress());
                        OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueBigFilesCompany);

                        // processAAFiles(companyTransPath, readyPath -> aaFilesService.processAATransactionFiles(company, readyPath, true));
                        processAAFiles(companyTransPath, readyPath -> placeInQueue(company, readyPath, inQueueCompany, inQueueBigFilesCompany, FILE_TYPE_AA_TRANSACTION));
                    } else {
                        Path inQueueCompany = bagsInQueueDir.resolve(company.getIpAddress());
                        OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);

                        Path inQueueBigFilesCompany = bagsInQueueBigFilesDir.resolve(company.getIpAddress());
                        OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueBigFilesCompany);

                        // processAAFiles(companyTransPath, readyPath -> aaFilesService.processAABagFiles(company, readyPath, true));
                        processAAFiles(companyTransPath, readyPath -> placeInQueue(company, readyPath, inQueueCompany, inQueueBigFilesCompany, FILE_TYPE_AA_BAG));
                    }

                }
            } catch (IOException e) {
                log.warn("Processing files failed", e);
            }
        });

        return true;
    }

    private void processAAFiles(Path fromDir, Consumer<Path> consumer) throws IOException {
        try (Stream<Path> paths = Files.find(fromDir, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && (fileName.endsWith(".ready"));
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                consumer.accept(file);
                // log.info("Processed from {}", file.toString());
            });
        }
    }

    private void placeInQueue(Company company, Path file, Path inQueueCompany, Path inQueueBigFilesCompany, String type) {
        String fileNameBase = getFilename(file);
        final Path parent = file.getParent();
        AAFiles aaFiles = getAAFiles(file, parent, fileNameBase);
        try {
            if (Files.size(aaFiles.getSlsPath()) / 1024 > 50) {
                moveFilesToQueue(inQueueBigFilesCompany, aaFiles);

                if (Files.exists(inQueueBigFilesCompany.resolve(file.getFileName().toString()))) {
                    publisherTransactionImportBigFiles.publishToQueue(new TransactionFilePayload(file.getFileName().toString(), company.getId(), type));
                    log.info("Published to the transaction/bag file import (Big files) queue {}", file.getFileName());
                }
            } else {
                moveFilesToQueue(inQueueCompany, aaFiles);

                if (Files.exists(inQueueCompany.resolve(file.getFileName().toString()))) {
                    publisherTransactionImport.publishToQueue(new TransactionFilePayload(file.getFileName().toString(), company.getId(), type));
                    log.info("Published to the transaction/bag file import queue {}", file.getFileName());
                }
            }
        } catch (IOException e) {
            log.warn("Processing AA file {} size failed", file.getFileName(), e);
        }
    }

    private void moveFilesToQueue(Path inQueueDir, AAFiles aaFiles) {
        moveIfExists(inQueueDir, aaFiles.getReadyPath());
        moveIfExists(inQueueDir, aaFiles.getReadyHashPath());
        moveIfExists(inQueueDir, aaFiles.getBatchPath());
        moveIfExists(inQueueDir, aaFiles.getBatchHashPath());
        moveIfExists(inQueueDir, aaFiles.getSlsPath());
        moveIfExists(inQueueDir, aaFiles.getSlsHashPath());
        moveIfExists(inQueueDir, aaFiles.getNlsPath());
        moveIfExists(inQueueDir, aaFiles.getNlsHashPath());
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.AAFilesPerCompanyImporter;
    }
}
