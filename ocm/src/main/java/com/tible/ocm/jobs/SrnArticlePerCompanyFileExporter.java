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
import com.tible.ocm.models.MaterialTypeCode;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.OcmVersion;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.SrnArticle;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.SrnArticleService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tible.hawk.core.utils.ExportHelper.writeValues;
import static com.tible.ocm.models.CommunicationType.*;
import static com.tible.ocm.models.CsvRecordType.*;
import static com.tible.ocm.models.OcmVersion.*;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

/**
 * It will go over all the company ip address folders and export the article file to them.
 */
@Component
@Slf4j
public class SrnArticlePerCompanyFileExporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final SrnArticleService articleService;

    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION, AH_TOMRA);

    public SrnArticlePerCompanyFileExporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                            BaseSettingsService<BaseSettings> settingsService,
                                            BaseMailService mailService,
                                            ConsulClient consulClient,
                                            DirectoryService directoryService,
                                            CompanyService companyService,
                                            SrnArticleService articleService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.articleService = articleService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getArticlesExportPath())) {
            log.error("Creating articles export directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnArticlePerCompanyFileExporter"), key = "task.SrnArticlePerCompanyFileExporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    @Scheduled(cron = "${tasks.srn-article-per-company-file-exporter}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        Path articleFilesPath = directoryService.getArticlesExportPath();
        Arrays.asList(OcmVersion.values()).forEach(ocmVersion -> {
            try {
                createArticleFile(articleFilesPath, ocmVersion);
                createHashFile(articleFilesPath, ocmVersion);
            } catch (IOException e) {
                log.warn("Could not create article file for version {}", ocmVersion, e);
            }
        });
        exportArticleFile();

        return true;
    }

    private void createArticleFile(Path path, OcmVersion ocmVersion) throws IOException {
        List<Integer> materials = new ArrayList<>();
        materials.add(MaterialTypeCode.PET.getCodeInt());
        if (ImportedFileValidationHelper.version17Check(ocmVersion.title)) {
            materials.add(MaterialTypeCode.STEEL.getCodeInt());
            materials.add(MaterialTypeCode.ALUMINIUM.getCodeInt());
        }

        List<SrnArticle> articles = articleService.getAllMaterialIn(materials);
        try (BufferedWriter writer = Files.newBufferedWriter(
                path.resolve(ARTICLE_EXPORT.concat(ocmVersion.title).concat(CSV_FILE_FORMAT)),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE)) {

            writeValues(writer, HDR.title,
                    ocmVersion.title,
                    LocalDateTime.now().format(DATETIMEFORMATTER));

            for (SrnArticle article : articles) {
                if (ImportedFileValidationHelper.version16Check(ocmVersion.title)) {
                    writeValues(writer, POS.title,
                            article.getNumber(),
                            article.getSupplier(),
                            formatMakeActiveOnDate(article.getActivationDate()),
                            Integer.toString(article.getWeight()),
                            Integer.toString(article.getVolume()),
                            Integer.toString(article.getHeight()),
                            Integer.toString(article.getDiameter()),
                            Integer.toString(article.getMaterial()),
                            Integer.toString(article.getDepositValue()),
                            article.getDescription(),
                            Integer.toString(article.getDepositCode()),
                            formatMakeActiveOnDate(article.getFirstArticleActivationDate()),
                            article.getColor(),
                            article.getShapeIdentifier() == null ? "" : article.getShapeIdentifier());
                } else {
                    writeValues(writer, POS.title,
                            article.getNumber(),
                            article.getSupplier(),
                            formatMakeActiveOnDate(article.getActivationDate()),
                            Integer.toString(article.getWeight()),
                            Integer.toString(article.getVolume()),
                            Integer.toString(article.getHeight()),
                            Integer.toString(article.getDiameter()),
                            Integer.toString(article.getMaterial()),
                            Integer.toString(article.getDepositValue()),
                            article.getDescription());
                }
            }

            writeValues(writer, SUM.title,
                    String.valueOf(articles.size()));
        }
    }

    private String formatMakeActiveOnDate(LocalDateTime makeActiveOn) {
        return makeActiveOn != null ? makeActiveOn.toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE) + "000000" : "";
    }

    private void createHashFile(Path path, OcmVersion ocmVersion) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path.resolve(ARTICLE_EXPORT.concat(ocmVersion.title).concat(HASH_FILE_FORMAT)), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writeValues(writer, false, FileUtils.getSha256HexFromFile(path.resolve(ARTICLE_EXPORT.concat(ocmVersion.title).concat(CSV_FILE_FORMAT))));
        }
    }

    private void exportArticleFile() {
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
                if (ImportedFileValidationHelper.version17Check(company.getVersion())) {
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_EXPORT.concat(VERSION_17.title).concat(CSV_FILE_FORMAT)), ARTICLE_EXPORT_CSV);
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_EXPORT.concat(VERSION_17.title).concat(HASH_FILE_FORMAT)), ARTICLE_EXPORT_HASH);
                } else if (ImportedFileValidationHelper.version162Check(company.getVersion())) {
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_EXPORT.concat(VERSION_162.title).concat(CSV_FILE_FORMAT)), ARTICLE_EXPORT_CSV);
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_EXPORT.concat(VERSION_162.title).concat(HASH_FILE_FORMAT)), ARTICLE_EXPORT_HASH);
                } else {
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_EXPORT.concat(VERSION_15.title).concat(CSV_FILE_FORMAT)), ARTICLE_EXPORT_CSV);
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_EXPORT.concat(VERSION_15.title).concat(CSV_FILE_FORMAT)), ARTICLE_EXPORT_HASH);
                }
                if (ImportedFileValidationHelper.version17Check(company.getVersion())) {
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_REMOVED_EXPORT_CSV), ARTICLE_REMOVED_EXPORT_CSV);
                    copyAndRenameIfExists(companyInputPath, directoryService.getArticlesExportPath()
                            .resolve(ARTICLE_REMOVED_EXPORT_HASH), ARTICLE_REMOVED_EXPORT_HASH);
                }
            }
        });
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnArticlePerCompanyFileExporter;
    }
}
