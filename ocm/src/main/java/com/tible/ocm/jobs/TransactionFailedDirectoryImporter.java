package com.tible.ocm.jobs;

import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.*;
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

import static com.tible.ocm.models.CommunicationType.*;

/**
 * Tries to import the transactions that are placed in the failed directory again.
 */
@Slf4j
@Component
public class TransactionFailedDirectoryImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final TransactionService transactionService;
    private final AAFilesService aaFilesService;
    private final RvmTransactionService rvmTransactionService;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG, SFTP, TOMRA_TRANSACTION, AH_TOMRA);
    private static final List<String> AA_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG);

    public TransactionFailedDirectoryImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                              BaseSettingsService settingsService,
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

   @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsPath())) {
            log.error("Creating transaction directory failed");
        }

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionsFailedPath())) {
            log.error("Creating transaction failed directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionFailedDirectoryImporter"), key = "task.TransactionFailedDirectoryImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.transaction-failed-directory-importer}")
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
                if (AA_COMMUNICATION_TYPES.contains(company.getCommunication())) {
                    try {
                        Path companyFailedPath = company.getCommunication().equals(AA_BAG) ?
                                directoryService.getBagsFailedPath().resolve(company.getIpAddress()) :
                                directoryService.getTransactionsFailedPath().resolve(company.getIpAddress());
                        if (Files.exists(companyFailedPath)) {
                            log.info("Trying to find failed AA transactions in failed directory for company with ip {}", company.getIpAddress());
                            getAAFailedFiles(companyFailedPath, readyPath -> aaFilesService.processAABackupOrFailedFiles(company, readyPath, true));
                            log.info("Placed AA failed files back to the company ip folder");
                        }
                    } catch (IOException e) {
                        log.warn("Getting failed files has failed", e);
                    }
                } else {
                    try {
                        Path companyFailedPath = directoryService.getTransactionsFailedPath().resolve(company.getIpAddress());
                        if (Files.exists(companyFailedPath)) {
                            log.info("Trying to find failed transactions in failed directory for company with ip {}", company.getIpAddress());
                            getTransactionFailedFiles(companyFailedPath, csvPath -> rvmTransactionService.processTransactionBackupOrFailedFiles(company, csvPath, true));
                            log.info("Placed failed files back to the company ip folder");
                        }
                    } catch (IOException e) {
                        log.warn("Getting failed files has failed", e);
                    }
                }
        }
        return true;
    }

    private void getAAFailedFiles(Path fromDir, Consumer<Path> consumer) throws IOException {
        try (Stream<Path> paths = Files.find(fromDir, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && (fileName.endsWith(".ready"));
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                consumer.accept(file);
                log.info("Got failed file from {}", file.toString());
            });
        }
    }

    private void getTransactionFailedFiles(Path fromDir, Consumer<Path> consumer) throws IOException {
        try (Stream<Path> paths = Files.find(fromDir, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && (fileName.endsWith(".csv"));
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                consumer.accept(file);
                log.info("Got failed file from {}", file.toString());
            });
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.TransactionFailedDirectoryImporter;
    }
}
