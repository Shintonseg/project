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
import com.tible.ocm.dto.helper.AAFiles;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mysql.ExistingBag;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.AA_BAG;
import static com.tible.ocm.models.CommunicationType.AA_TRANSACTION;
import static com.tible.ocm.models.mongo.RejectedTransaction.TransactionType.BAG;
import static com.tible.ocm.utils.ImportHelper.copyIfExists;
import static com.tible.ocm.utils.ImportHelper.getFilename;
import static com.tible.ocm.utils.OcmFileUtils.getAAFiles;

@Slf4j
@Component
public class SynchronizeRejectedBagsTask extends AbstractSynchronizeRejectedTask {

    private final ExistingBagService existingBagService;
    private final DirectoryService directoryService;
    private final CompanyService companyService;

    private static final List<String> ALLOWED_BAG_COMMUNICATION_TYPES = List.of(AA_BAG);
    private static final List<String> ALLOWED_TRANSACTION_COMMUNICATION_TYPES = List.of(AA_TRANSACTION);
    private static final String BAGS_REJECTED_DIRECTORIES_SYNCHRONIZED = "bagsRejectedDirectoriesSynchronized";
    private static final int ONE_WEEK_DAYS = 7;

    public SynchronizeRejectedBagsTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                       BaseSettingsService<BaseSettings> settingsService,
                                       BaseMailService mailService,
                                       ConsulClient consulClient,
                                       CompanyService companyService,
                                       DirectoryService directoryService,
                                       ExistingBagService existingBagService,
                                       ExistingTransactionService existingTransactionService,
                                       SynchronizedDirectoryService synchronizedDirectoryService,
                                       ObjectMapper objectMapper,
                                       RejectedTransactionService rejectedTransactionService,
                                       @Qualifier("redisClient") RedissonClient redissonClient) {
        super(taskService, settingsService, mailService, consulClient, companyService, directoryService,
                synchronizedDirectoryService, objectMapper, rejectedTransactionService, existingTransactionService,
                redissonClient);
        this.existingBagService = existingBagService;
        this.directoryService = directoryService;
        this.companyService = companyService;
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SynchronizeRejectedBagsTask"), key = "task.SynchronizeRejectedBagsTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.synchronize-rejected-bags-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path bagsRejectedPath = directoryService.getBagsRejectedPath();
        Path transactionsRejectedPath = directoryService.getTransactionsRejectedPath();
        String ocmFileOwner = settingsService.getValue("ocm-file-owner", "ocmuser");

        List<Company> companiesBag = getCompanies(ALLOWED_BAG_COMMUNICATION_TYPES);
        List<Company> companiesTransaction = getCompanies(ALLOWED_TRANSACTION_COMMUNICATION_TYPES);

        companiesBag.forEach(company -> {
            int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
            LocalDate dateInPast = LocalDate.now().minusDays(dataExpirationPeriodInDays).minusDays(ONE_WEEK_DAYS);

            Path companyIpRejectedPath = extractRejectedPathFromCompany(company);
            log.info("Remove existing bags their rejected bag (AA) files from {}", companyIpRejectedPath);
            List<String> existingCombinedCustomerNumberLabels = existingBagService.findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(company.getRvmOwnerNumber(), dateInPast)
                .stream()
                .map(ExistingBag::getCombinedCustomerNumberLabel)
                .collect(Collectors.toList());
            removeFoundFiles(companyIpRejectedPath, existingCombinedCustomerNumberLabels, BAG);

            List<String> rejectedTransactions = getRejectedTransactionsFileNames(company);
            removeRejectedTransactionsFiles(companyIpRejectedPath, rejectedTransactions);

            // log.info("Fix owner of bag files from {}", bagsRejectedPath.resolve(company.getNumber()));
            fixOwnerOfFiles(bagsRejectedPath.resolve(company.getNumber()));
        });

        companiesTransaction.forEach(company -> {
            Path companyIpRejectedPath = extractRejectedPathFromCompany(company);
            log.info("Remove existing rejected their transaction (AA bag based) files from {}", companyIpRejectedPath);
            List<String> existingNumbers = getExistingTransactionNumbers(company);
            removeFoundFiles(companyIpRejectedPath, existingNumbers, BAG);

            List<String> rejectedTransactions = getRejectedTransactionsFileNames(company);
            removeRejectedTransactionsFiles(companyIpRejectedPath, rejectedTransactions);

            // log.info("Fix owner of transaction files from {}", transactionsRejectedPath.resolve(company.getNumber()));
            fixOwnerOfFiles(transactionsRejectedPath.resolve(company.getNumber()));
        });

        Map<Path, Path> companiesBagsPathMap = companiesBag.stream()
                .collect(Collectors.toMap(key -> bagsRejectedPath.resolve(key.getNumber()), this::extractRejectedPathFromCompany));
        handleSynchronization(companiesBagsPathMap, BAGS_REJECTED_DIRECTORIES_SYNCHRONIZED);

        Map<Path, Path> companiesTransactionsPathMap = companiesTransaction.stream()
                .collect(Collectors.toMap(key -> transactionsRejectedPath.resolve(key.getNumber()), this::extractRejectedPathFromCompany));
        handleSynchronization(companiesTransactionsPathMap, BAGS_REJECTED_DIRECTORIES_SYNCHRONIZED);

        return true;
    }

    @Override
    protected void handleCustomSynchronization(Path path, Map.Entry<Path, Path> entry) {
        try {
            final Path parent = path.getParent();

            String fileNameBase = getFilename(path);
            AAFiles aaFiles = getAAFiles(path, parent, fileNameBase);

            Instant errorLastModifiedInstant = Files.readAttributes(aaFiles.getErrorFile(), BasicFileAttributes.class).lastModifiedTime().toInstant();
            Path errorFileIpDir = entry.getValue().resolve(fileNameBase + ".error");
            boolean errorFileIpExists = Files.exists(errorFileIpDir);
            if (!errorFileIpExists || errorLastModifiedInstant.isAfter(Files.readAttributes(errorFileIpDir, BasicFileAttributes.class).lastModifiedTime().toInstant())) {
                copyIfExists(entry.getValue(), aaFiles.getReadyPath(), aaFiles.getReadyHashPath(),
                        aaFiles.getBatchPath(), aaFiles.getBatchHashPath(), aaFiles.getSlsPath(), aaFiles.getSlsHashPath(),
                        aaFiles.getNlsPath(), aaFiles.getNlsHashPath(), aaFiles.getErrorFile());
                log.info("Synchronized {} rejected bag/transaction files to company ip directory {}", getFilename(path), path.getParent().getFileName().toString());
            }
        } catch (IOException e) {
            log.warn("Synchronized rejected bag/transaction error: {}", e.getMessage());
        }
    }

    @Override
    protected boolean filterFiles(BasicFileAttributes attributes, String fileName) {
        return !attributes.isDirectory() && fileName.endsWith(".ready");
    }

    @Override
    protected Predicate<Path> deleteRejectedFiles() {
        return RejectedFilesUtils::deleteRejectedBagFiles;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SynchronizeRejectedBagsTask;
    }
}
