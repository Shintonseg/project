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
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.RefundArticle;
import com.tible.ocm.services.ArticleService;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.RefundArticleService;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.models.CommunicationType.*;
import static com.tible.ocm.utils.ImportHelper.OUTPUT_DIRECTORY;

@Slf4j
@Component
public class RefundArticlePerCompanyFileImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final ArticleService articleService;
    private final CompanyService companyService;
    private final RefundArticleService refundArticleService;


    private static final List<String> ALLOWED_COMMUNICATION_TYPES = List.of(SFTP, TOMRA_TRANSACTION, AH_TOMRA);

    public RefundArticlePerCompanyFileImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                               BaseSettingsService<BaseSettings> settingsService,
                                               BaseMailService mailService,
                                               ConsulClient consulClient,
                                               DirectoryService directoryService,
                                               ArticleService articleService,
                                               CompanyService companyService,
                                               RefundArticleService refundArticleService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.articleService = articleService;
        this.companyService = companyService;
        this.refundArticleService = refundArticleService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getArticlesPath())) {
            log.error("Creating articles directory failed");
        }
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getArticlesFromPath())) {
            log.error("Creating articles from directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "RefundArticlePerCompanyFileImporter"), key = "task.RefundArticlePerCompanyFileImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    /**
     * Synchronize.
     */
    @Async
    // @Scheduled(cron = "${tasks.refund-article-per-company-file-importer}")
    @Override
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

        companies.forEach(company -> {
            try {
                Path companyPath = directoryService.getRoot().resolve(company.getIpAddress());
                Path companyOutputPath = companyPath.resolve(OUTPUT_DIRECTORY);
                if (Files.exists(companyPath) && Files.exists(companyOutputPath)) {
                    processArticlesFiles(company, companyOutputPath);
                }

            } catch (IOException e) {
                log.warn("Processing files failed", e);
            }
        });

        return true;
    }

    private void processArticlesFiles(Company company, Path companyOutputPath) throws IOException {
        try (Stream<Path> paths = Files.find(companyOutputPath, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".csv");
        })) {
            List<RefundArticle> refundArticles = refundArticleService.findAllByCompanyId(company.getId());
            // Collecting necessary, otherwise it goes wrong on linux (caching it seems)
            paths.collect(Collectors.toList()).forEach(file -> {
                articleService.processArticleFile(company.getNumber(), company.getVersion(), refundArticles,
                        company.getIpAddress(), file, true, company.getCommunication());
                log.info("Processed from {}", file.toString());
            });
        }
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.RefundArticlePerCompanyFileImporter;
    }
}
