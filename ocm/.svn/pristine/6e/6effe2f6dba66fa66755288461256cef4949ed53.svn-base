package com.tible.ocm.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.configurations.ConsulClient;
import com.tible.hawk.core.jobs.CommonTask;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.models.mongo.RejectedTransaction;
import com.tible.ocm.models.mongo.SynchronizedDirectory;
import com.tible.ocm.services.*;
import com.tible.ocm.utils.OcmFileUtils;
import com.tible.ocm.utils.RejectedFilesUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.utils.ImportHelper.*;

@Slf4j
public abstract class AbstractSynchronizeRejectedTask extends CommonTask<BaseTask, BaseTaskParameter> {

    private final CompanyService companyService;
    private final DirectoryService directoryService;
    private final SynchronizedDirectoryService synchronizedDirectoryService;
    private final ObjectMapper objectMapper;
    private final RejectedTransactionService rejectedTransactionService;
    private final ExistingTransactionService existingTransactionService;
    private final RedissonClient redissonClient;

    private static final String FILE_NAME = "synchronized.json";
    private static final int ONE_WEEK_DAYS = 7;

    protected AbstractSynchronizeRejectedTask(final BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                                              final BaseSettingsService<BaseSettings> settingsService,
                                              final BaseMailService mailService,
                                              final ConsulClient consulClient,
                                              final CompanyService companyService,
                                              final DirectoryService directoryService,
                                              final SynchronizedDirectoryService synchronizedDirectoryService,
                                              final ObjectMapper objectMapper,
                                              final RejectedTransactionService rejectedTransactionService,
                                              ExistingTransactionService existingTransactionService,
                                              @Qualifier("redisClient") final RedissonClient redissonClient) {
        super(taskService, settingsService, mailService, consulClient);
        this.companyService = companyService;
        this.directoryService = directoryService;
        this.synchronizedDirectoryService = synchronizedDirectoryService;
        this.objectMapper = objectMapper;
        this.rejectedTransactionService = rejectedTransactionService;
        this.existingTransactionService = existingTransactionService;
        this.redissonClient = redissonClient;
    }

    protected List<Company> getCompanies(List<String> allowedCommunicationTypes) {
        return companyService.findAll().stream()
                .filter(Objects::nonNull)
                .filter(company -> !StringUtils.isEmpty(company.getIpAddress()))
                .filter(company -> !StringUtils.isEmpty(company.getCommunication()))
                .filter(company -> allowedCommunicationTypes.contains(company.getCommunication()))
                .collect(Collectors.toList());
    }

    protected List<String> getRejectedTransactionsFileNames(Company company) {
        return rejectedTransactionService
                .findAllNeedToBeDeletedByCompanyNumber(company.getNumber())
                .stream()
                .map(RejectedTransaction::getBaseFileName)
                .collect(Collectors.toList());
    }

    protected List<String> getExistingTransactionNumbers(Company company) {
        int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
        LocalDate dateInPast = LocalDate.now().minusDays(dataExpirationPeriodInDays).minusDays(ONE_WEEK_DAYS);
        return existingTransactionService.findAllByRvmOwnerNumberAndCreatedDateIsGreaterThanEqual(company.getRvmOwnerNumber(), dateInPast)
                .stream()
                .map(ExistingTransaction::getNumber)
                .collect(Collectors.toList());
    }

    protected void removeFoundFiles(Path from, List<String> existingNumbers, RejectedTransaction.TransactionType type) {
        AtomicInteger deletedFoundFiles = new AtomicInteger();
        try {
            if (OcmFileUtils.checkOrCreateDirWithFullPermissions(from)) {
                try (Stream<Path> paths = Files.find(from, 1, (path, attributes) -> {
                    String fileName = path.getFileName().toString();
                    return !attributes.isDirectory() && fileName.endsWith(".error") &&
                            existingNumbers.contains(getFilename(path));
                })) {
                    // Collecting necessary, otherwise it goes wrong on linux (caching it seems)
                    paths.collect(Collectors.toList())
                            .stream()
                            .filter(deleteRejectedFiles())
                            .forEach(path -> {
                                deleteRejectedTransaction(getFilename(path), type);
                                deletedFoundFiles.getAndIncrement();
                            });
                }
            }
        } catch (IOException e) {
            log.warn("Failed to check existing transaction with files to remove", e);
        }
        log.info("Deleted {} found transaction/bag (type: {}) their files", deletedFoundFiles, type);
    }

    protected void handleSynchronization(Map<Path, Path> companiesFilesPathMap, String cacheName) {
        List<Path> alreadySynchronized = new ArrayList<>();
        companiesFilesPathMap.keySet().stream()
                .filter(Files::exists)
                .forEach(key -> {
                    if (Files.exists(key.resolve(FILE_NAME))) {
                        try {
                            //String json = new String(Files.readAllBytes(key.resolve(FILE_NAME)));
                            String synchronizedDateTimeString = objectMapper.readValue(FileUtils.readFileToString(key.resolve(FILE_NAME).toFile(), StandardCharsets.UTF_8), String.class);
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                            LocalDateTime synchronizedDateTime = LocalDateTime.parse(synchronizedDateTimeString.substring(0, synchronizedDateTimeString.lastIndexOf(".")), formatter);

                            if (synchronizedDirectoryService.existsByName(key.getFileName().toString())) {
                                SynchronizedDirectory synchronizedDirectory = synchronizedDirectoryService.findByName(key.getFileName().toString());
                                if (synchronizedDateTime.isEqual(synchronizedDirectory.getDateTime())) {
                                    log.info("Company directory {} is already Synchronized", key.getFileName().toString());
                                    alreadySynchronized.add(key);
                                } else {
                                    synchronizedDirectory.setDateTime(synchronizedDateTime);
                                    synchronizedDirectoryService.save(synchronizedDirectory);
                                }
                            } else {
                                SynchronizedDirectory synchronizedDirectory = new SynchronizedDirectory();
                                synchronizedDirectory.setName(key.getFileName().toString());
                                synchronizedDirectory.setDateTime(synchronizedDateTime);
                                synchronizedDirectoryService.save(synchronizedDirectory);
                            }
                        } catch (IOException e) {
                            log.warn("Synchronized rejected bag/transaction error: {}", e.getMessage());
                        }
                    }
                });

        RLocalCachedMap<String, LocalDateTime> cache = redissonClient.
                getLocalCachedMap(cacheName, LocalCachedMapOptions.defaults());
        companiesFilesPathMap.entrySet().stream()
                .filter(entry -> Files.exists(entry.getKey()))
                .filter(entry -> cache == null || cache.get(entry.getKey().toString()) == null ||
                        !alreadySynchronized.contains(entry.getKey()) ||
                        (cache.get(entry.getKey().toString()) != null &&
                                cache.get(entry.getKey().toString()).plusHours(1).isBefore(LocalDateTime.now())))
                .forEach(entry -> {
                    try (Stream<Path> paths = Files.find(entry.getKey(), 1, (path, attributes) -> {
                        String fileName = path.getFileName().toString();
                        return filterFiles(attributes, fileName);
                    })) {
                        paths.collect(Collectors.toList())
                                .forEach(path -> handleCustomSynchronization(path, entry));
                    } catch (IOException e) {
                        log.warn("Synchronized rejected bags/transactions error: {}", e.getMessage());
                    }

                    if (cache != null) {
                        cache.put(entry.getKey().toString(), LocalDateTime.now());
                    }
                });
    }

    protected void fixOwnerOfFiles(Path companyNumberRejectedPath) {
        String ocmFileOwner = settingsService.getValue("ocm-file-owner", "ocmuser");
        FileSystem fileSystem = FileSystems.getDefault();
        UserPrincipalLookupService userPrincipalLookupService = fileSystem.getUserPrincipalLookupService();
        try {
            UserPrincipal ocmOwner = userPrincipalLookupService.lookupPrincipalByName(ocmFileOwner);

            if (OcmFileUtils.checkOrCreateDirWithFullPermissions(companyNumberRejectedPath)) {
                try (Stream<Path> paths = Files.find(companyNumberRejectedPath, 1, (path, attributes) ->
                        !attributes.isDirectory())) {
                    // Collecting necessary, otherwise it goes wrong on linux (caching it seems)
                    paths.collect(Collectors.toList()).forEach(path -> {
                        FileOwnerAttributeView fileOwnerAttributeView = Files.getFileAttributeView(path,
                                FileOwnerAttributeView.class);

                        try {
                            UserPrincipal originalOwner = fileOwnerAttributeView.getOwner();
                            if (!originalOwner.getName().equals(ocmFileOwner)) {
                                fileOwnerAttributeView.setOwner(ocmOwner);
                            }
                        } catch (IOException e) {
                            log.warn("Could not set new owner", e);
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.warn("Could not find ocm file owner {}", ocmFileOwner);
        }
    }

    protected void removeRejectedTransactionsFiles(Path from, List<String> rejectedTransactions) {
        try {
            if (OcmFileUtils.checkOrCreateDirWithFullPermissions(from)) {
                try (Stream<Path> paths = Files.find(from, 1, (path, attributes) -> {
                    String fileName = path.getFileName().toString();
                    return !attributes.isDirectory() && fileName.endsWith(".error") && rejectedTransactions.contains(getFilename(path));
                })) {
                    // Collecting necessary, otherwise it goes wrong on linux (caching it seems)
                    paths.collect(Collectors.toList())
                            .forEach(RejectedFilesUtils::deleteRejectedTransactionFiles);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to check rejected transaction with files to remove", e);
        }
    }

    protected void deleteRejectedTransaction(String fileNameBase, RejectedTransaction.TransactionType type) {
        List<RejectedTransaction> rejectedTransactions = rejectedTransactionService
                .findAllByBaseFileNameAndType(fileNameBase, type);
        rejectedTransactionService.deleteAll(rejectedTransactions);
    }

    protected Path extractRejectedPathFromCompany(Company company) {
        Path companyPath = directoryService.getRoot().resolve(company.getIpAddress());
        Path companyBagsPath = companyPath.resolve(TRANS_DIRECTORY); // Yeah, bags is using TRANS directory at the ip directory side.
        OcmFileUtils.checkOrCreateDirWithFullPermissions(companyBagsPath);
        Path rejectedPathFromCompany = companyBagsPath.resolve(REJECTED_DIRECTORY);
        OcmFileUtils.checkOrCreateDirWithFullPermissions(rejectedPathFromCompany);
        return rejectedPathFromCompany;
    }

    protected abstract Predicate<Path> deleteRejectedFiles();

    protected abstract boolean filterFiles(BasicFileAttributes attributes, String fileName);

    protected abstract void handleCustomSynchronization(Path path, Map.Entry<Path, Path> entry);
}
