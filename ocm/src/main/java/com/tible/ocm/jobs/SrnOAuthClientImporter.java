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
import com.tible.hawk.core.utils.ZipFileHelper;
import com.tible.ocm.dto.OAuthClientDto;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.OAuthClientService;
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
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.hawk.core.utils.EncoderUtils.decodeValue;
import static com.tible.ocm.services.log.LogKeyConstant.OAUTHCLIENTS_KEY;

@Slf4j
@Component
public class SrnOAuthClientImporter extends CommonTask<BaseTask, BaseTaskParameter> {

    private final DirectoryService directoryService;
    private final OAuthClientService oauthClientService;
    private final LogExporterService<LogFileInfo> loggerExporterService;
    private final ConversionService conversionService;
    private final ZipFileHelper zipFileHelper;
    private final ObjectMapper objectMapper;

    private static final String ZIP_EXTENSION = ".zip";

    @Value("${ocm.secret-key}")
    private String key;

    @Value("${tible-user.username}")
    private String tibleUsername;
    @Value("${tible-admin-user.username}")
    private String tibleAdminUsername;
    @Value("${lamson-user.username}")
    private String lamsonUsername;
    @Value("${aldi-user.username}")
    private String aldiUsername;

    public SrnOAuthClientImporter(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                  BaseSettingsService<BaseSettings> settingsService,
                                  BaseMailService mailService,
                                  ConsulClient consulClient,
                                  DirectoryService directoryService,
                                  OAuthClientService oauthClientService,
                                  LogExporterService<LogFileInfo> loggerExporterService,
                                  ConversionService conversionService,
                                  ZipFileHelper zipFileHelper) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.oauthClientService = oauthClientService;
        this.loggerExporterService = loggerExporterService;
        this.conversionService = conversionService;
        this.zipFileHelper = zipFileHelper;

        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(directoryService.getOAuthClientsExportPath())) {
            log.error("Creating oAuthClients directory failed");
        }
    }

    @Override
    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "SrnOAuthClientImporter"), key = "task.SrnOAuthClientImporter", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC))})
    public void receiveMessage(@Payload final TaskMessage message) {
        super.receiveMessage(message);
    }

    @Async
    @Scheduled(cron = "${tasks.srn-oauth-clients-importer}")
    @Override
    public void schedule() {
        super.schedule();
    }

    @Override
    protected boolean toExecute(final BaseTask processTask) {
        final List<String> internalClients = List.of(tibleUsername, tibleAdminUsername, lamsonUsername, aldiUsername);
        final Path oAuthClientsExportDir = directoryService.getOAuthClientsExportPath();
        unzipSecuredFile(oAuthClientsExportDir);

        LogFileInfo logFileInfo = LogFileInfo.builder()
                .isNeedExport(true)
                .fileName("oAuthClients")
                .path(directoryService.getOAuthClientsLogPath())
                .build();

        List<String> oAuthClientsLogs = new ArrayList<>();
        boolean failed = false;
        try (Stream<Path> paths = Files.find(oAuthClientsExportDir, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(".json");
        })) {
            paths.collect(Collectors.toList()).forEach(file -> {
                List<OAuthClient> clients = readSrnOAuthClients(file);
                Optional<OAuthClient> adminUser = oauthClientService.findByClientId(tibleUsername);
                adminUser.ifPresent(clients::add);
                Optional<OAuthClient> tibleAdminUser = oauthClientService.findByClientId(tibleAdminUsername);
                tibleAdminUser.ifPresent(clients::add);
                Optional<OAuthClient> lamsonUser = oauthClientService.findByClientId(lamsonUsername);
                lamsonUser.ifPresent(clients::add);
                Optional<OAuthClient> aldiUser = oauthClientService.findByClientId(aldiUsername);
                aldiUser.ifPresent(clients::add);
                oauthClientService.remove(oauthClientService.findAllNotIn(clients));
                clients.forEach(oauthClient -> {
                    if (!internalClients.contains(oauthClient.getClientId())) {
                        OAuthClient savedOAuthClient = oauthClientService.save(oauthClient);
                        log.info(String.format("OAuthClient %s was saved successfully", savedOAuthClient.getClientId()));
                        oAuthClientsLogs.add(String.format("OAuthClient %s was saved successfully", savedOAuthClient.getClientId()));
                    }
                });
                deleteFile(file);
            });
        } catch (Exception e) {
            log.warn("Process oauthclients.json failed", e);
            failed = true;
        }

        loggerExporterService.exportWithDetailMessage(OAUTHCLIENTS_KEY, oAuthClientsLogs,
                failed ? "OAuthClients file was not handled successfully" : "OAuthClients file was handled successfully", logFileInfo);
        return true;
    }

    private void unzipSecuredFile(Path oAuthClientsExportDir) {
        try (Stream<Path> paths = Files.find(oAuthClientsExportDir, 2, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(ZIP_EXTENSION);
        })) {
            paths.collect(Collectors.toList()).forEach(file -> zipFileHelper.unzipWithPassword(file, Boolean.FALSE));
        } catch (IOException e) {
            log.warn("Failed to unzip oauthclients.zip", e);
        }
    }

    private void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete oauthclients.json", e);
        }
    }

    private List<OAuthClient> readSrnOAuthClients(Path file) {
        List<OAuthClient> clients = Lists.newArrayList();

        try {
            // String json = new String(Files.readAllBytes(file));

            clients = Arrays.stream(objectMapper.readValue(FileUtils.readFileToString(file.toFile(),
                            StandardCharsets.UTF_8), OAuthClientDto[].class))
                    .map(this::convertTo)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("Reading oauthclients.json failed", e);
        }

        return clients;
    }

    private OAuthClient convertTo(OAuthClientDto oauthClientDto) {
        oauthClientDto.setClientSecret(decodeValue(oauthClientDto.getClientSecret(), key));
        return conversionService.convert(oauthClientDto, OAuthClient.class);
    }

    @Override
    public String getTaskName() {
        return OcmTaskType.SrnOAuthClientImporter;
    }

}
