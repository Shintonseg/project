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
import com.tible.ocm.dto.ExistingTransactionDto;
import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.ExistingTransactionLatestService;
import com.tible.ocm.services.ExistingTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractExistingTransactionsImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final ExistingTransactionService existingTransactionService;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;
    private final ExistingTransactionLatestService existingTransactionLatestService;

    public AbstractExistingTransactionsImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                                BaseSettingsService<BaseSettings> settingsService,
                                                BaseMailService mailService,
                                                ConsulClient consulClient,
                                                ExistingTransactionService existingTransactionService,
                                                ConversionService conversionService,
                                                ExistingTransactionLatestService existingTransactionLatestService) {
        super(taskService, settingsService, mailService, consulClient);
        this.existingTransactionService = existingTransactionService;
        this.conversionService = conversionService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.existingTransactionLatestService = existingTransactionLatestService;
    }

    protected void processFiles(Path existingTransactionsExportPath, boolean skipLatestTable) {
        List<Long> savedExistingTransactionIds = new ArrayList<>();
        List<String> savedLatestExistingTransactionIds = new ArrayList<>();

        try (Stream<Path> paths = Files.find(existingTransactionsExportPath, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".json");
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                String rvmOwnerNumberFromFile = getRvmOwnerNumberFromFileName(file);

                try (BufferedReader bufferedReader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    ExistingTransactionDto[] existingTransactionsDto = objectMapper.readValue(bufferedReader, ExistingTransactionDto[].class);
                    for (ExistingTransactionDto existingTransactionDto : existingTransactionsDto) {
                        ExistingTransaction existingTransaction = convertTo(existingTransactionDto);
                        existingTransaction.setRvmOwnerNumber(rvmOwnerNumberFromFile);
                        if (!existingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(existingTransaction.getNumber(),
                                existingTransaction.getRvmOwnerNumber())) {
                            savedExistingTransactionIds.add(existingTransactionService.save(existingTransaction).getId());
                        }

                        if (!skipLatestTable &&
                                !existingTransactionLatestService.existsByTransactionNumberAndRvmOwnerNumber(existingTransaction.getNumber(), existingTransaction.getRvmOwnerNumber())) {
                            savedLatestExistingTransactionIds.add(existingTransactionLatestService.saveExistingTransaction(existingTransaction).getId());
                        }
                    }
                } catch (IOException e) {
                    log.warn("Processing existing transactions file {} failed", file, e);
                }

            });
        } catch (IOException e) {
            log.warn("Processing existing transactions files failed", e);
        }

        log.info("Saved {} existingTransaction entities", savedExistingTransactionIds.size());
        log.info("Saved {} latest existingTransaction entities", savedLatestExistingTransactionIds.size());
    }

    private String getRvmOwnerNumberFromFileName(Path pathToFile) {
        String fileName = pathToFile.getFileName().toString();
        if (fileName.lastIndexOf("-") > 0) {
            return fileName.substring(fileName.lastIndexOf("-") + 1, fileName.lastIndexOf("."));
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    private ExistingTransaction convertTo(ExistingTransactionDto existingTransactionDto) {
        return conversionService.convert(existingTransactionDto, ExistingTransaction.class);
    }
}
