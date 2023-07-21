package com.tible.ocm.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.dto.RejectedTransactionDto;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.RejectedTransactionService;
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.models.CommunicationType.*;

@Slf4j
@Component
public class SrnRejectedTransactionExporterTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final CompanyService companyService;
    private final RejectedTransactionService rejectedTransactionService;
    private final DirectoryService directoryService;
    private final ObjectMapper objectMapper;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION, AH_TOMRA,
            AA_BAG, AA_TRANSACTION, REST, AH_CLOUD);
    private static final String REJECTED_TRANSACTIONS_FILE_NAME = "rejectedTransactions.json";

    public SrnRejectedTransactionExporterTask(final BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                              final BaseSettingsService<BaseSettings> settingsService,
                                              final BaseMailService mailService,
                                              final ConsulClient consulClient,
                                              final CompanyService companyService,
                                              final RejectedTransactionService rejectedTransactionService,
                                              final DirectoryService directoryService,
                                              final ObjectMapper objectMapper) {
        super(taskService, settingsService, mailService, consulClient);
        this.companyService = companyService;
        this.rejectedTransactionService = rejectedTransactionService;
        this.directoryService = directoryService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getRejectedTransactionsPath())) {
            log.error("Creating rejected transactions directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnRejectedTransactionExporterTask"), key = "task.SrnRejectedTransactionExporterTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Override
    @Scheduled(cron = "${tasks.rejected-transaction-exporter-task}")
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        List<Company> companies = getCompanies();

        List<RejectedTransactionDto> rejectedTransactions = companies.stream()
                .map(Company::getNumber)
                .filter(Objects::nonNull)
                .map(rejectedTransactionService::findAllByCompanyNumber)
                .flatMap(Collection::stream)
                .distinct()
                .map(RejectedTransactionDto::from)
                .collect(Collectors.toList());

        Path rejectedTransactionsPath = directoryService.getRejectedTransactionsPath();
        Path filePath = rejectedTransactionsPath.resolve(REJECTED_TRANSACTIONS_FILE_NAME);

        try {
            String json = objectMapper.writeValueAsString(rejectedTransactions);
            createFile(filePath, json);
        } catch (Exception e) {
            log.error("Failed to write rejected transactions to JSON", e);
        }

        fixOwnerOfFiles(rejectedTransactionsPath);

        return true;
    }

    protected void fixOwnerOfFiles(Path fixPath) {
        String ocmFileOwner = settingsService.getValue("ocm-file-owner", "ocmuser");
        FileSystem fileSystem = FileSystems.getDefault();
        UserPrincipalLookupService userPrincipalLookupService = fileSystem.getUserPrincipalLookupService();
        try {
            UserPrincipal ocmOwner = userPrincipalLookupService.lookupPrincipalByName(ocmFileOwner);

            if (OcmFileUtils.checkOrCreateDirWithFullPermissions(fixPath)) {
                try (Stream<Path> paths = Files.find(fixPath, 1, (path, attributes) ->
                        !attributes.isDirectory())) {
                    // Collecting necessary, otherwise it goes wrong on linux (caching it seems)
                    paths.collect(Collectors.toList()).forEach(path -> {
                        FileOwnerAttributeView fileOwnerAttributeView = Files.getFileAttributeView(path,
                                FileOwnerAttributeView.class);

                        try {
                            UserPrincipal originalOwner = fileOwnerAttributeView.getOwner();
                            if (!originalOwner.getName().equals(ocmFileOwner)) {
                                fileOwnerAttributeView.setOwner(ocmOwner);
                            }
                        } catch (IOException e) {
                            log.warn("Could not set new owner", e);
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.warn("Could not find ocm file owner {}", ocmFileOwner);
        }
    }

    private List<Company> getCompanies() {
        return companyService.findAll()
                .stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> ALLOWED_COMMUNICATION_TYPES.contains(company.getCommunication()))
                .collect(Collectors.toList());
    }

    private void createFile(Path file, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content);
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnRejectedTransactionExporterTask;
    }
}
