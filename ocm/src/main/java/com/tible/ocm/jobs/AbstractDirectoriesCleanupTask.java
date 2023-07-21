package com.tible.ocm.jobs;

import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.exceptions.DeleteFileException;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractDirectoriesCleanupTask extends CommonTask<BaseTask, BaseTaskParameter> {

    protected final DirectoryService directoryService;
    protected final CompanyService companyService;

    public AbstractDirectoriesCleanupTask(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                          BaseSettingsService<BaseSettings> settingsService,
                                          BaseMailService mailService,
                                          ConsulClient consulClient,
                                          DirectoryService directoryService,
                                          CompanyService companyService) {
        super(taskService, settingsService, mailService, consulClient);
        this.directoryService = directoryService;
        this.companyService = companyService;
    }

    protected void cleanupExpiredFilesFromIpDirectory(Path dir) {
        try (Stream<Path> paths = Files.find(dir, 2, (path, attributes) -> {
            if (attributes.isDirectory()) {
                return false;
            }
            String companyIp = path.getParent().getFileName().toString();
            Company company = companyService.findFirstByIpAddress(companyIp);
            int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
            LocalDate creationDate = LocalDate.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
            return LocalDate.now().minusDays(dataExpirationPeriodInDays).isAfter(creationDate);
        })) {
            paths.collect(Collectors.toList()).forEach(this::deleteFile);
        } catch (IOException e) {
            log.warn("Directories clean up task failed", e);
        }
    }

    protected void cleanupExpiredFilesFromDirectory(Path dir, Company company) {
        try (Stream<Path> paths = Files.find(dir, 2, (path, attributes) -> {
            if (attributes.isDirectory()) {
                return false;
            }
            int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
            LocalDate creationDate = LocalDate.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
            return LocalDate.now().minusDays(dataExpirationPeriodInDays).isAfter(creationDate);
        })) {
            paths.collect(Collectors.toList()).forEach(this::deleteFile);
        } catch (IOException e) {
            log.warn("Directories clean up task failed", e);
        }
    }

    protected void cleanupExpiredFilesFromCompanyNumberDirectory(Path dir) {
        try (Stream<Path> paths = Files.find(dir, 2, (path, attributes) -> {
            if (attributes.isDirectory()) {
                return false;
            }
            String companyNumber = path.getParent().getFileName().toString();
            Company company = companyService.findByNumber(companyNumber);
            int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
            LocalDate creationDate = LocalDate.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
            return LocalDate.now().minusDays(dataExpirationPeriodInDays).isAfter(creationDate);
        })) {
            paths.collect(Collectors.toList()).forEach(this::deleteFile);
        } catch (IOException e) {
            log.warn("Directories clean up task failed", e);
        }
    }

    protected void deleteFile(Path file) {
        try {
            Files.delete(file);
            log.info("Deleted file {}", file);
        } catch (IOException e) {
            log.warn("Failed to delete file {}", file.getFileName(), e);
            throw new DeleteFileException(file.getFileName().toString());
        }
    }
}
