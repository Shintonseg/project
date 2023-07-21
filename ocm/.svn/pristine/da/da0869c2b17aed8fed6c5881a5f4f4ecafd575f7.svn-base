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
import com.tible.ocm.dto.LabelOrderDto;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.LabelOrder;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.LabelOrderService;
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
import java.io.BufferedReader;
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
public class SrnLabelOrdersImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;
    private final LabelOrderService labelOrderService;

    public SrnLabelOrdersImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                  BaseSettingsService<BaseSettings> settingsService,
                                  BaseMailService mailService,
                                  ConsulClient consulClient,
                                  DirectoryService directoryService,
                                  ConversionService conversionService,
                                  ObjectMapper objectMapper,
                                  LabelOrderService labelOrderService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.conversionService = conversionService;
        this.objectMapper = objectMapper;
        this.labelOrderService = labelOrderService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getLabelOrdersPath())) {
            log.error("Creating label orders directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnLabelOrdersImporter"), key = "task.SrnLabelOrdersImporter",
                    exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-label-orders-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path labelOrdersPath = directoryService.getLabelOrdersPath();
        processFiles(labelOrdersPath);
        return true;
    }

    protected void processFiles(Path labelOrdersPath) {
        List<String> savedLabelOrderIds = new ArrayList<>();
        List<String> updatedLabelOrderIds = new ArrayList<>();

        try (Stream<Path> paths = Files.find(labelOrdersPath, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".json");
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                try (BufferedReader bufferedReader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    LabelOrderDto[] labelOrdersDto = objectMapper.readValue(bufferedReader, LabelOrderDto[].class);
                    for (LabelOrderDto labelOrderDto : labelOrdersDto) {
                        LabelOrder labelOrder = convertTo(labelOrderDto);
                        if (labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(labelOrder.getCustomerNumber(),
                                labelOrder.getRvmOwnerNumber(), labelOrder.getFirstLabelNumber())) {
                            LabelOrder updatedLabelOrder = labelOrderService.findByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(labelOrder.getCustomerNumber(),
                                    labelOrder.getRvmOwnerNumber(), labelOrder.getFirstLabelNumber());
                            updatedLabelOrder.setCustomerLocalizationNumber(labelOrder.getCustomerLocalizationNumber());
                            updatedLabelOrder.setBalance(labelOrder.getBalance());
                            updatedLabelOrder.setOrderDate(labelOrder.getOrderDate());
                            updatedLabelOrder.setMarkAllLabelsAsUsed(labelOrder.getMarkAllLabelsAsUsed());
                            LabelOrder savedLabelOrder = labelOrderService.save(updatedLabelOrder);
                            updatedLabelOrderIds.add(savedLabelOrder.getId());
                        } else {
                            LabelOrder savedLabelOrder = labelOrderService.save(labelOrder);
                            savedLabelOrderIds.add(savedLabelOrder.getId());
                        }
                    }
                } catch (IOException e) {
                    log.warn("Processing label order file {} failed", file, e);
                }
            });
        } catch (IOException e) {
            log.warn("Processing label orders files failed", e);
        }

        log.info("Saved {} label order entities", savedLabelOrderIds.size());
        log.info("Updated {} label order entities", updatedLabelOrderIds.size());
    }

    private LabelOrder convertTo(LabelOrderDto labelOrderDto) {
        return conversionService.convert(labelOrderDto, LabelOrder.class);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnLabelOrdersImporter;
    }
}
