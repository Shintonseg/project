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
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TransactionsConsistencyCheck extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Value("${transaction-check-delay-days}")
    private int dayOffset;
    @Value("${transaction-check-delay-hours}")
    private int hoursOffset;

    private static final String FILE_NAME = "transactionnumbers.json";

    public TransactionsConsistencyCheck(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                        BaseSettingsService<BaseSettings> settingsService,
                                        BaseMailService mailService,
                                        ConsulClient consulClient,
                                        DirectoryService directoryService,
                                        TransactionRepository transactionRepository,
                                        ObjectMapper objectMapper) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getTransactionNumbersExportDir())) {
            log.error("Creating transactionNumbers directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionsConsistencyCheck"),
                    key = "task.TransactionsConsistencyCheck",
                    exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Override
    @Scheduled(cron = "${tasks.transactions-consistency-check}")
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path exportDir = directoryService.getTransactionNumbersExportDir();

        LocalDateTime fromDate = LocalDate.now().atStartOfDay().minusDays(dayOffset);
        LocalDateTime toDate = LocalDateTime.now().minusHours(hoursOffset);
        List<String> transactions = transactionRepository
                .findAllByDateTimeGreaterThanEqualAndReceivedDateLessThanEqual(fromDate, toDate)
                .stream()
                .map(Transaction::getTransactionNumber)
                .collect(Collectors.toList());

        try {
            String jsonValue = objectMapper.writeValueAsString(transactions);
            createFile(exportDir.resolve(FILE_NAME), jsonValue);
        } catch (Exception e) {
            log.error("Failed to write transaction numbers to JSON", e);
            return false;
        }

        return true;
    }

    private void createFile(Path exportFile, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content);
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.TransactionsConsistencyCheck;
    }
}
