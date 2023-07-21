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
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.log.LogExporterService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tible.ocm.models.CommunicationType.AA_BAG;
import static com.tible.ocm.models.CommunicationType.AA_TRANSACTION;
import static com.tible.ocm.utils.ImportHelper.*;

@Component
@Slf4j
public class SrnArticlesPricatPerCompanyFileExporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final LogExporterService<LogFileInfo> loggerExporterService;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(AA_TRANSACTION, AA_BAG);

    public SrnArticlesPricatPerCompanyFileExporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                                   BaseSettingsService<BaseSettings> settingsService,
                                                   BaseMailService mailService,
                                                   ConsulClient consulClient,
                                                   DirectoryService directoryService,
                                                   CompanyService companyService,
                                                   LogExporterService<LogFileInfo> loggerExporterService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.loggerExporterService = loggerExporterService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getArticlesPricatExportPath())) {
            log.error("Creating articles pricat export directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnArticlesPricatPerCompanyFileExporter"), key = "task.SrnArticlesPricatPerCompanyFileExporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.srn-articles-pricat-per-company-file-exporter}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path articlesPricatPath = directoryService.getArticlesPricatExportPath();

        Map<String, Object> logContent = new HashMap<>();
        LogFileInfo.LogFileInfoBuilder logFileInfoBuilder = LogFileInfo.builder()
                .isNeedExport(true)
                .fileName("articlesPricatExport")
                .path(directoryService.getArticlePricatLogPath());

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
                Path articlePricatTxt = articlesPricatPath.resolve(ARTICLES_PRICAT_EXPORT + "_" + company.getVersion() + ARTICLES_PRICAT_EXPORT_TXT);
                copyAndRenameIfExists(companyInputPath, articlePricatTxt, ARTICLES_PRICAT_EXPORT + ARTICLES_PRICAT_EXPORT_TXT);
                Path articlePricatHash = articlesPricatPath.resolve(ARTICLES_PRICAT_EXPORT + "_" + company.getVersion() + ARTICLES_PRICAT_EXPORT_HASH);
                copyAndRenameIfExists(companyInputPath, articlePricatHash, ARTICLES_PRICAT_EXPORT + ARTICLES_PRICAT_EXPORT_HASH);

                logContent.put(company.getIpAddress(), String.format("Article pricat file %s was exported successfully", articlePricatTxt.getFileName()));
            }
        });

        loggerExporterService.logToFile(logFileInfoBuilder.build(), logContent);

        return true;
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnArticlesPricatPerCompanyFileExporter;
    }
}
