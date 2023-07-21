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
import com.tible.ocm.models.OcmVersion;
import com.tible.ocm.models.mongo.Company;
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tible.hawk.core.utils.ExportHelper.writeValues;
import static com.tible.ocm.models.CommunicationType.*;
import static com.tible.ocm.models.CsvRecordType.*;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

@Component
@Slf4j
public class SrnCharitiesFileExporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION, AH_TOMRA);

    public SrnCharitiesFileExporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                    BaseSettingsService<BaseSettings> settingsService,
                                    BaseMailService mailService,
                                    ConsulClient consulClient,
                                    DirectoryService directoryService,
                                    CompanyService companyService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getCharitiesExportPath())) {
            log.error("Creating charities export directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnCharitiesFileExporter"), key = "task.SrnCharitiesFileExporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.srn-charities-file-exporter}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path charitiesFilesPath = directoryService.getCharitiesExportPath();
        try {
            createCharitiesFile(charitiesFilesPath);
            createHashFile(charitiesFilesPath);
        } catch (IOException e) {
            log.warn("Could not create charities file", e);
        }
        exportCharitiesFile();

        return true;
    }

    private void createCharitiesFile(Path path) throws IOException {
        List<Company> charities = companyService.findAllCharities();
        try (BufferedWriter writer = Files.newBufferedWriter(
                path.resolve(CHARITIES_EXPORT.concat(CSV_FILE_FORMAT)),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE)) {

            writeValues(writer, HDR.title,
                    OcmVersion.VERSION_17.title,
                    LocalDateTime.now().format(DATETIMEFORMATTER));

            for (Company charity : charities) {
                writeValues(writer, POS.title,
                        charity.getNumber(),
                        charity.getName());
            }

            writeValues(writer, SUM.title,
                    String.valueOf(charities.size()));
        }
    }

    private void createHashFile(Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path.resolve(CHARITIES_EXPORT.concat(HASH_FILE_FORMAT)), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writeValues(writer, false, FileUtils.getSha256HexFromFile(path.resolve(CHARITIES_EXPORT.concat(CSV_FILE_FORMAT))));
        }
    }

    private void exportCharitiesFile() {
        List<Company> companies = companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> ALLOWED_COMMUNICATION_TYPES.contains(company.getCommunication()))
                .collect(Collectors.toList());

        companies.forEach(company -> {
            Path companyPath = directoryService.getRoot().resolve(company.getIpAddress());
            Path companyInputPath = companyPath.resolve(INPUT_DIRECTORY);
            if (Files.exists(companyPath) && Files.exists(companyInputPath)) {
                copyAndRenameIfExists(companyInputPath, directoryService.getCharitiesExportPath()
                        .resolve(CHARITIES_EXPORT.concat(CSV_FILE_FORMAT)), CHARITIES_EXPORT_CSV);
                copyAndRenameIfExists(companyInputPath, directoryService.getCharitiesExportPath()
                        .resolve(CHARITIES_EXPORT.concat(HASH_FILE_FORMAT)), CHARITIES_EXPORT_HASH);
            }
        });
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnCharitiesFileExporter;
    }
}
