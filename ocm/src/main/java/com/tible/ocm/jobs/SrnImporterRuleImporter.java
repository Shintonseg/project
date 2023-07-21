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
import com.tible.ocm.dto.ImporterRuleDto;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.ImporterRule;
import com.tible.ocm.models.mongo.ImporterRuleLimitations;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.ImporterRuleService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
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

import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class SrnImporterRuleImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final ConversionService conversionService;
    private final ImporterRuleService importerRuleService;
    private final ObjectMapper objectMapper;

    public SrnImporterRuleImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                   BaseSettingsService<BaseSettings> settingsService,
                                   BaseMailService mailService,
                                   ConsulClient consulClient,
                                   DirectoryService directoryService,
                                   ConversionService conversionService,
                                   ImporterRuleService importerRuleService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.conversionService = conversionService;
        this.importerRuleService = importerRuleService;

        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getImporterRuleExportPath())) {
            log.error("Creating importer rule directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnImporterRuleImporter"), key = "task.SrnImporterRuleImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-importer-rule-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        log.info("starting importer rule importer task");
        final Path importerRuleExportPath = directoryService.getImporterRuleExportPath();

        try (Stream<Path> paths = Files.find(importerRuleExportPath, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".json");
        })) {
            paths
                    .collect(Collectors.toList())
                    .forEach(this::handleImporterRules);
        } catch (IOException e) {
            log.warn("Process importerRule.json failed", e);
        }

        return true;
    }

    private void handleImporterRules(Path file) {
        List<ImporterRuleDto> importerRuleDtoList = readImporterRule(file);
        List<ImporterRule> savedRules = new ArrayList<>();

        importerRuleDtoList.forEach(importerRuleDto -> {
            List<ImporterRuleLimitations> importerRuleLimitations = mapImporterRuleLimitations(importerRuleDto);

            ImporterRule importerRule = new ImporterRule();
            importerRule.setFromEan(importerRuleDto.getFromEan());
            importerRule.setToEan(importerRuleDto.getToEan());
            importerRule.setArticleDescription(importerRuleDto.getArticleDescription());

            importerRuleService
                    .findByFromEan(importerRule.getFromEan())
                    .ifPresentOrElse(updatedImporterRule -> {
                                updatedImporterRule.setFromEan(importerRule.getFromEan());
                                updatedImporterRule.setToEan(importerRule.getToEan());
                                updatedImporterRule.setArticleDescription(importerRule.getArticleDescription());
                                ImporterRule saved = importerRuleService.save(updatedImporterRule, importerRuleLimitations);
                                savedRules.add(saved);
                            },
                            () -> {
                                ImporterRule saved = importerRuleService.save(importerRule, importerRuleLimitations);
                                savedRules.add(saved);
                            });
        });

        importerRuleService.remove(importerRuleService.findAllNotIn(savedRules));
    }

    private List<ImporterRuleDto> readImporterRule(Path file) {
        List<ImporterRuleDto> importerRules = Lists.newArrayList();

        try {
            // String json = new String(Files.readAllBytes(file));

            importerRules = Arrays.stream(objectMapper.readValue(FileUtils.readFileToString(file.toFile(),
                            StandardCharsets.UTF_8), ImporterRuleDto[].class))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Reading importerRule.json failed", e);
        }

        return importerRules;
    }

    private List<ImporterRuleLimitations> mapImporterRuleLimitations(ImporterRuleDto importerRuleDto) {
        return ofNullable(importerRuleDto.getImporterRuleLimitations())
                .map(limitations -> limitations
                        .stream()
                        .map(it -> conversionService.convert(it, ImporterRuleLimitations.class))
                        .collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnImporterRuleImporter;
    }
}
