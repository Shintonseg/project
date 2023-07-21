package com.tible.ocm.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.RejectedTransactionService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class SrnRejectedTransactionImporterTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final RejectedTransactionService rejectedTransactionService;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;

    private static final String REJECTED_TRANSACTIONS_REMOVED_FILE_NAME = "rejectedTransactionsRemoved.json";

    public SrnRejectedTransactionImporterTask(final BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                              final BaseSettingsService<BaseSettings> settingsService,
                                              final BaseMailService mailService,
                                              final ConsulClient consulClient,
                                              final DirectoryService directoryService,
                                              final RejectedTransactionService rejectedTransactionService,
                                              final ConversionService conversionService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.rejectedTransactionService = rejectedTransactionService;
        this.conversionService = conversionService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getRejectedTransactionsPath())) {
            log.error("Creating rejected transactions directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnRejectedTransactionImporterTask"), key = "task.SrnRejectedTransactionImporterTask", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.rejected-transaction-importer-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path rejectedTransactionsPath = directoryService.getRejectedTransactionsPath();

        try (Stream<Path> paths = Files.find(rejectedTransactionsPath, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.equals(REJECTED_TRANSACTIONS_REMOVED_FILE_NAME);
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                List<RejectedTransaction> rejectedTransactions = readRejectedTransactionsFile(file);
                rejectedTransactions.forEach(rejectedTransaction -> {
                    if (rejectedTransactionService
                            .existsByCompanyNumberAndCreatedAtAndBaseFileName(
                                    rejectedTransaction.getCompanyNumber(),
                                    rejectedTransaction.getCreatedAt(),
                                    rejectedTransaction.getBaseFileName())) {

                        RejectedTransaction updated = rejectedTransactionService
                                .findByCompanyNumberAndCreatedAtAndBaseFileName(
                                        rejectedTransaction.getCompanyNumber(),
                                        rejectedTransaction.getCreatedAt(),
                                        rejectedTransaction.getBaseFileName());
                        updated.setNeedToBeDeleted(rejectedTransaction.isNeedToBeDeleted());
                        updated.setDeletedSince(rejectedTransaction.getDeletedSince());
                        rejectedTransactionService.save(updated);
                    } else {
                        rejectedTransaction.setExternal(Boolean.TRUE);
                        rejectedTransactionService.save(rejectedTransaction);
                    }
                });
            });
        } catch (Exception e) {
            log.warn("Process {} failed", REJECTED_TRANSACTIONS_REMOVED_FILE_NAME, e);
        }

        return true;
    }

    private List<RejectedTransaction> readRejectedTransactionsFile(Path file) {
        List<RejectedTransaction> rejectedTransactions = new ArrayList<>();

        try {
            // String json = new String(Files.readAllBytes(file));

            rejectedTransactions = Arrays.stream(objectMapper.readValue(FileUtils.readFileToString(file.toFile(),
                            StandardCharsets.UTF_8), RejectedTransactionDto[].class))
                    .map(this::convertTo)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Reading {} failed", REJECTED_TRANSACTIONS_REMOVED_FILE_NAME, e);
        }

        return rejectedTransactions;
    }

    private RejectedTransaction convertTo(RejectedTransactionDto rejectedTransactionDto) {
        return conversionService.convert(rejectedTransactionDto, RejectedTransaction.class);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnRejectedTransactionImporterTask;
    }
}
