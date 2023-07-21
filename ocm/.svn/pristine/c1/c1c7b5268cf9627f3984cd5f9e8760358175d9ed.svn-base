package com.tible.ocm.jobs;

import com.google.common.base.Strings;
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
import com.tible.ocm.models.CsvRecordType;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.SrnArticle;
import com.tible.ocm.models.mongo.SrnRemovedArticle;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.SrnArticleService;
import com.tible.ocm.services.SrnRemovedArticleService;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tible.hawk.core.utils.ImportHelper.readFile;
import static com.tible.ocm.services.log.LogKeyConstant.ARTICLES_KEY;
import static com.tible.ocm.utils.ImportHelper.ARTICLE_EXPORT_CSV;
import static com.tible.ocm.utils.ImportHelper.ARTICLE_REMOVED_EXPORT_CSV;

@Slf4j
@Component
public class SrnArticleFileImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final SrnArticleService srnArticleService;
    private final SrnRemovedArticleService srnRemovedArticleService;
    private final LogExporterService<LogFileInfo> loggerExporterService;

    public SrnArticleFileImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                  BaseSettingsService<BaseSettings> settingsService,
                                  BaseMailService mailService,
                                  ConsulClient consulClient,
                                  DirectoryService directoryService,
                                  SrnArticleService srnArticleService,
                                  SrnRemovedArticleService srnRemovedArticleService, LogExporterService<LogFileInfo> loggerExporterService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.srnArticleService = srnArticleService;
        this.srnRemovedArticleService = srnRemovedArticleService;
        this.loggerExporterService = loggerExporterService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getArticlesExportPath())) {
            log.error("Creating articlesExport directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnArticleFileImporter"), key = "task.SrnArticleFileImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-article-file-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path articlesExportDir = directoryService.getArticlesExportPath();

        LogFileInfo logFileInfo = LogFileInfo.builder()
                .isNeedExport(true)
                .fileName("articles")
                .path(directoryService.getArticlesLogPath())
                .build();

        List<String> articlesLogs = new ArrayList<>();
        boolean failed = false;
        if (Files.exists(articlesExportDir.resolve(ARTICLE_EXPORT_CSV))) {
            Path articlesFile = articlesExportDir.resolve(ARTICLE_EXPORT_CSV);
            Path deactivatedArticlesFile = articlesExportDir.resolve(ARTICLE_REMOVED_EXPORT_CSV);
            synchronizeArticlesInFileAndDb(articlesLogs, articlesFile);
            synchronizeRemovedArticlesInFileAndDb(deactivatedArticlesFile);
        } else {
            log.warn("Process articles.csv failed");
            failed = true;
        }

        loggerExporterService.exportWithDetailMessage(ARTICLES_KEY, articlesLogs,
                failed ? "Articles file was not handled successfully" : "Articles file was handled successfully", logFileInfo);
        return true;
    }

    private void synchronizeArticlesInFileAndDb(List<String> articlesLogs, Path file) {
        List<SrnArticle> srnArticles = readSrnArticles(file);

        if (srnArticles.isEmpty()) {
            return;
        }

        srnArticleService.saveSrnArticles(srnArticles);
        log.info("Processed {} active articles from file {}", srnArticles.size(), file.toString());
        srnArticles.forEach(
                article -> articlesLogs.add(String.format("Article %s was saved successfully", article.getNumber())));

        List<String> fileSrnArticleEans = srnArticles.stream()
                .map(SrnArticle::getNumber)
                .collect(Collectors.toList());
        List<SrnArticle> allExistingArticles = srnArticleService.findAllArticles();
        List<SrnArticle> articlesNotFoundInFile = allExistingArticles.stream()
                .filter(srnArticle -> !fileSrnArticleEans.contains(srnArticle.getNumber()))
                .collect(Collectors.toList());
        srnArticleService.deleteAll(articlesNotFoundInFile);
        log.info("Deleted {} not existing active articles eans at database compared to eans from file {}",
                articlesNotFoundInFile.size(), file.toString());
        articlesNotFoundInFile.forEach(
                article -> articlesLogs.add(String.format("Article %s was deleted successfully", article.getNumber())));
    }

    private void synchronizeRemovedArticlesInFileAndDb(Path file) {
        List<SrnRemovedArticle> removedArticles = readRemovedArticlesFromFile(file);

        if (removedArticles.isEmpty()) {
            return;
        }

        srnRemovedArticleService.saveSrnRemovedArticles(removedArticles);
        log.info("Processed {} removed articles from file {}", removedArticles.size(), file.toString());
        List<String> eansFromFile = removedArticles.stream()
                .map(SrnRemovedArticle::getNumber)
                .collect(Collectors.toList());
        List<SrnRemovedArticle> existingRemovedArticles = srnRemovedArticleService.findAll();
        List<SrnRemovedArticle> removedArticlesNotFoundInFile = existingRemovedArticles.stream()
                .filter(article -> !eansFromFile.contains(article.getNumber()))
                .collect(Collectors.toList());
        srnRemovedArticleService.deleteAll(removedArticlesNotFoundInFile);
        log.info("Deleted {} not existing removed articles eans at database compared to eans from file {}",
                removedArticlesNotFoundInFile.size(), file.toString());
    }

    private List<SrnArticle> readSrnArticles(Path file) {
        List<SrnArticle> srnArticles = Lists.newArrayList();
        try {
            readFile(file, (scanner, type) -> {
                switch (CsvRecordType.valueOf(type)) {
                    case POS:
                        SrnArticle article = new SrnArticle();
                        article.setNumber(scanner.next());
                        article.setSupplier(scanner.next());
                        LocalDate activationDate = parseDate(scanner.next());
                        if (activationDate != null) {
                            article.setActivationDate(activationDate.atTime(0, 0, 0, 0));
                        }
                        article.setWeight(Integer.parseInt(scanner.next()));
                        article.setVolume(Integer.parseInt(scanner.next()));
                        article.setHeight(Integer.parseInt(scanner.next()));
                        article.setDiameter(Integer.parseInt(scanner.next()));
                        article.setMaterial(Integer.parseInt(scanner.next()));
                        article.setDepositValue(Integer.parseInt(scanner.next()));
                        article.setDescription(scanner.next());
                        if (scanner.hasNext()) {
                            article.setDepositCode(Integer.parseInt(scanner.next()));
                        }
                        if (scanner.hasNext()) {
                            LocalDate firstArticleActivationDate = parseDate(scanner.next());
                            if (firstArticleActivationDate != null) {
                                article.setFirstArticleActivationDate(firstArticleActivationDate.atTime(0, 0, 0, 0));
                            }
                        }
                        if (scanner.hasNext()) {
                            article.setColor(scanner.next());
                        }
                        if (scanner.hasNext()) {
                            article.setShapeIdentifier(scanner.next());
                        }
                        srnArticles.add(article);
                        break;
                }
            });
        } catch (IOException e) {
            log.warn("Reading articles.csv failed", e);
        }
        return srnArticles;
    }

    private List<SrnRemovedArticle> readRemovedArticlesFromFile(Path file) {
        List<SrnRemovedArticle> removedArticles = new LinkedList<>();
        try {
            readFile(file, (scanner, type) -> {
                if (CsvRecordType.valueOf(type) == CsvRecordType.POS) {
                    String ean = scanner.next();
                    LocalDate deactivationDate = parseDate(scanner.next());
                    if (deactivationDate != null) {
                        removedArticles.add(SrnRemovedArticle.builder()
                                .number(ean)
                                .deactivationDate(deactivationDate.atTime(0, 0, 0, 0))
                                .build());
                    }
                }
            });
        } catch (IOException e) {
            log.warn("Reading article-removed.csv failed", e);
        }
        return removedArticles;
    }

    private LocalDate parseDate(String date) {
        return Strings.isNullOrEmpty(date) ? null :
                LocalDate.parse(date.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
    }


    @Override
    public String getTaskName() {
        return OcmTaskType.SrnArticleFileImporter;
    }
}
