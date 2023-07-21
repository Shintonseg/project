package com.tible.ocm.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.jobs.TaskMessage;
import com.tible.hawk.core.models.BaseEventLog;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseEventLogService;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.RejectedTransactionService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class RejectedTransactionCleanUpTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final ObjectMapper objectMapper;
    private final RejectedTransactionService rejectedTransactionService;
    private final BaseEventLogService<BaseEventLog> eventLogService;

    public RejectedTransactionCleanUpTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                          BaseSettingsService<BaseSettings> settingsService,
                                          BaseMailService mailService,
                                          ConsulClient consulClient,
                                          DirectoryService directoryService,
                                          ObjectMapper objectMapper,
                                          RejectedTransactionService rejectedTransactionService,
                                          BaseEventLogService<BaseEventLog> eventLogService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.objectMapper = objectMapper;
        this.rejectedTransactionService = rejectedTransactionService;
        this.eventLogService = eventLogService;
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "RejectedTransactionCleanUpTask"), key = "task.RejectedTransactionCleanUpTask",
                    exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.rejected-transaction-clean-up-task}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path rejectedTransactionsToBeRemovedDir = directoryService.getRejectedTransactionsToBeRemovedDir();

        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedTransactionsToBeRemovedDir)) {
            log.error("Creating rejected transactions to be removed directory failed");
            return false;
        }

        try (Stream<Path> paths = Files.walk(rejectedTransactionsToBeRemovedDir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(pathToFile -> {
                        try {
                            String rejectedTransactionNumber = getTransactionNumberFromFile(pathToFile);

                            if (!rejectedTransactionNumber.isBlank()) {
                                removeFilesFromOcm(rejectedTransactionNumber);
                                removeFromDB(rejectedTransactionNumber);
                                Files.delete(pathToFile);
                            }
                        } catch (IOException e) {
                            log.error("Failed to delete file");
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to retrieve files");
        }
        return true;
    }

    private String getTransactionNumberFromFile(Path pathToFile) {
        try {
            String fileContent = new String(Files.readAllBytes(pathToFile));
            Map<String, String> map = objectMapper.readValue(fileContent, Map.class);
            return map.getOrDefault("transactionToBeRemoved", "");
        } catch (IOException e) {
            log.error("Failed to get transaction number from file");
            return "";
        }
    }

    private void removeFilesFromOcm(String fileName) {
        List<Path> paths = getAllFilesByName(fileName);

        if (!paths.isEmpty()) {
            paths.forEach(transactionFile -> {
                try {
                    Files.delete(transactionFile);
                } catch (IOException e) {
                    log.error("Failed to delete file from OCM {}", transactionFile.getFileName(), e);
                }
            });
        }
    }

    private void removeFromDB(String fileName) {
        String transactionNumber = getTransactionNumberFromFileName(fileName);
        List<RejectedTransaction> rejectedTransactions = rejectedTransactionService
                .findAllByBaseFileNameAndType(transactionNumber, RejectedTransaction.TransactionType.TRANSACTION);
        List<RejectedTransaction> rejectedBags = rejectedTransactionService
                .findAllByBaseFileNameAndType(transactionNumber, RejectedTransaction.TransactionType.BAG);

        if (!rejectedTransactions.isEmpty()) {
            rejectedTransactionService.deleteAll(rejectedTransactions);
            eventLogService.createDelete(rejectedTransactions);
        }
        if (!rejectedBags.isEmpty()) {
            rejectedTransactionService.deleteAll(rejectedBags);
            eventLogService.createDelete(rejectedBags);
        }
    }

    private String getTransactionNumberFromFileName(String fileName) {
        if (fileName.contains("-")) {
            return fileName.substring(0, fileName.lastIndexOf("-"));
        } else {
            return fileName;
        }
    }

    private List<Path> getAllFilesByName(String fileName) {
        Path rootDir = directoryService.getRoot();

        try (Stream<Path> stream = Files.walk(rootDir, Integer.MAX_VALUE)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains(fileName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to get files", e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.RejectedTransactionCleanUpTask;
    }
}