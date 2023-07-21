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
import com.tible.ocm.dto.CompanyDto;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.log.LogExporterService;
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

import static com.tible.ocm.services.log.LogKeyConstant.COMPANIES_KEY;

@Slf4j
@Component
public class SrnCompanyImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final ConversionService conversionService;
    private final ObjectMapper objectMapper;
    private final LogExporterService<LogFileInfo> loggerExporterService;

    public SrnCompanyImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                              BaseSettingsService<BaseSettings> settingsService,
                              BaseMailService mailService,
                              ConsulClient consulClient,
                              DirectoryService directoryService,
                              CompanyService companyService,
                              ConversionService conversionService,
                              ObjectMapper objectMapper,
                              LogExporterService<LogFileInfo> loggerExporterService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.conversionService = conversionService;
        this.objectMapper = objectMapper;
        this.loggerExporterService = loggerExporterService;
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getCompaniesExportPath())) {
            log.error("Creating company directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnCompanyImporter"), key = "task.SrnCompanyImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-company-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final Path companiesExportPath = directoryService.getCompaniesExportPath();

        LogFileInfo logFileInfo = LogFileInfo.builder()
                .isNeedExport(true)
                .fileName("companies")
                .path(directoryService.getCompaniesLogPath())
                .build();

        List<String> companiesLogs = new ArrayList<>();
        boolean failed = false;
        try (Stream<Path> paths = Files.find(companiesExportPath, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".json");
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                List<Company> companies = readCompany(file);
                companies.forEach(company -> {
                    if (companyService.existsByNumber(company.getNumber())) {
                        Company updatedCompany = companyService.findByNumber(company.getNumber());
                        updatedCompany.setName(company.getName());
                        updatedCompany.setType(company.getType());
                        updatedCompany.setIpRange(company.getIpRange());
                        updatedCompany.setSerialNumbers(company.getSerialNumbers());
                        updatedCompany.setIpAddress(company.getIpAddress());
                        updatedCompany.setUsingIpTrunking(company.isUsingIpTrunking());
                        updatedCompany.setStoreId(company.getStoreId());
                        updatedCompany.setVersion(company.getVersion());
                        updatedCompany.setCommunication(company.getCommunication());
                        updatedCompany.setRvmOwnerNumber(company.getRvmOwnerNumber());
                        updatedCompany.setLocalizationNumber(company.getLocalizationNumber());
                        updatedCompany.setAllowDataYoungerThanDays(company.getAllowDataYoungerThanDays());
                        updatedCompany.setNotifyAboutDoubleTransactions(company.isNotifyAboutDoubleTransactions());
                        Company savedCompany = companyService.save(updatedCompany);
                        companiesLogs.add(String.format("Company %s was edited successfully", savedCompany.getNumber()));
                    } else {
                        Company savedCompany = companyService.save(company);
                        companiesLogs.add(String.format("Company %s was saved successfully", savedCompany.getNumber()));
                    }
                });
                // String filename = getFilename(file);
            });
        } catch (Exception e) {
            log.warn("Process companies.json failed", e);
            failed = true;
        }

        loggerExporterService.exportWithDetailMessage(COMPANIES_KEY, companiesLogs,
                failed ? "Company file was not handled successfully" : "Company file was handled successfully", logFileInfo);
        return true;
    }

    private List<Company> readCompany(Path file) {
        List<Company> companies = Lists.newArrayList();

        try {
            // String json = new String(Files.readAllBytes(file));

            companies = Arrays.stream(objectMapper.readValue(FileUtils.readFileToString(file.toFile(),
                            StandardCharsets.UTF_8), CompanyDto[].class))
                    .map(this::convertTo)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Reading companies.json failed", e);
        }

        return companies;
    }

    private Company convertTo(CompanyDto companyDto) {
        return conversionService.convert(companyDto, Company.class);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnCompanyImporter;
    }
}
