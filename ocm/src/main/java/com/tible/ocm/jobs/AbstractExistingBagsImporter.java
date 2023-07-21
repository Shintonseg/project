package com.tible.ocm.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.dto.ExistingBagDto;
import com.tible.ocm.models.mysql.ExistingBag;
import com.tible.ocm.services.ExistingBagLatestService;
import com.tible.ocm.services.ExistingBagService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.springframework.core.convert.ConversionService;

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
public abstract class AbstractExistingBagsImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    protected final ExistingBagService existingBagService;
    protected final ExistingBagLatestService existingBagLatestService;
    private final ObjectMapper objectMapper;
    private final ConversionService conversionService;

    protected AbstractExistingBagsImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                           BaseSettingsService<BaseSettings> settingsService,
                                           BaseMailService mailService,
                                           ConsulClient consulClient,
                                           ExistingBagService existingBagService,
                                           ExistingBagLatestService existingBagLatestService,
                                           ConversionService conversionService) {
        super(taskService, settingsService, mailService, consulClient);
        this.existingBagService = existingBagService;
        this.existingBagLatestService = existingBagLatestService;
        this.conversionService = conversionService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    protected void processFiles(Path existingBagsExportPath, boolean skipLatestTable) {
        List<Long> savedExistingBagIds = new ArrayList<>();
        List<String> savedLatestExistingBagIds = new ArrayList<>();

        try (Stream<Path> paths = Files.find(existingBagsExportPath, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".json");
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                List<ExistingBag> existingBags = readExistingBags(file);
                String rvmOwnerNumberFromFile = getRvmOwnerNumberFromFileName(file);
                log.debug("Got rvm owner number from file {} is {}", file.getFileName().toString(), rvmOwnerNumberFromFile);

                for (ExistingBag existingBag : existingBags) {
                    if (!existingBagService.existsByCombinedCustomerNumberLabel(existingBag.getCombinedCustomerNumberLabel())) {
                        savedExistingBagIds.add(saveWithRvmOwnerNumber(existingBag, rvmOwnerNumberFromFile));
                    }

                    if (!skipLatestTable &&
                            !existingBagLatestService.existsByCombinedCustomerNumberLabel(existingBag.getCombinedCustomerNumberLabel())) {
                        savedLatestExistingBagIds.add(existingBagLatestService.saveExistingBag(existingBag).getId());
                    }
                }
            });
        } catch (IOException e) {
            log.warn("Processing existing transactions files failed", e);
        }

        log.info("Saved {} existingBag entities", savedExistingBagIds.size());
        log.info("Saved {} latest existingBag entities", savedLatestExistingBagIds.size());
    }

    protected List<ExistingBag> readExistingBags(Path file) {
        List<ExistingBag> existingBags = Lists.newArrayList();

        try {
            existingBags = Arrays.stream(objectMapper.readValue(FileUtils.readFileToString(file.toFile(),
                            StandardCharsets.UTF_8), ExistingBagDto[].class))
                    .map(this::convertTo)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Reading {} failed", file.getFileName().toString(), e);
        }

        return existingBags;
    }

    protected Long saveWithRvmOwnerNumber(ExistingBag existingBag, String rvmOwnerNumberFromFile) {
        existingBag.setRvmOwnerNumber(rvmOwnerNumberFromFile);
        return existingBagService.save(existingBag).getId();
    }

    private String getRvmOwnerNumberFromFileName(Path pathToFile) {
        String fileName = pathToFile.getFileName().toString();
        return fileName.substring(fileName.lastIndexOf("-") + 1, fileName.lastIndexOf("."));
    }

    private ExistingBag convertTo(ExistingBagDto existingBagDto) {
        return conversionService.convert(existingBagDto, ExistingBag.class);
    }
}
