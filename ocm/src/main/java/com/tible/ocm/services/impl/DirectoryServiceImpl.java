package com.tible.ocm.services.impl;

import com.tible.hawk.core.configurations.Finder;
import com.tible.hawk.core.models.BaseDirectory;
import com.tible.hawk.core.models.BaseDocument;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.repositories.BaseDirectoryRepository;
import com.tible.hawk.core.repositories.BaseDocumentRepository;
import com.tible.hawk.core.services.BaseEventLogService;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.impl.AbstractBaseDirectoryService;
import com.tible.hawk.core.services.impl.BaseAuthUtils;
import com.tible.ocm.services.DirectoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Primary
@Service
public class DirectoryServiceImpl extends AbstractBaseDirectoryService<BaseDirectory, BaseDocument> implements DirectoryService {

    private final Finder finder;

    private final BaseAuthUtils authUtils;

    @Value("${paths.rvm-dir}")
    private String rvmDir;

    @Value("${paths.transactions-dir}")
    private String transactionsDir;

    @Value("${paths.transactions-from-dir}")
    private String transactionsFromDir;

    @Value("${paths.transactions-backup-dir}")
    private String transactionsBackupDir;

    @Value("${paths.transactions-rejected-dir}")
    private String transactionsRejectedDir;

    @Value("${paths.transactions-failed-dir}")
    private String transactionsFailedDir;

    @Value("${paths.transactions-accepted-dir}")
    private String transactionsAcceptedDir;

    @Value("${paths.transactions-confirmed-dir}")
    private String transactionsConfirmedDir;

    @Value("${paths.transactions-already-exists-dir}")
    private String transactionsAlreadyExistsDir;

    @Value("${paths.transactions-in-queue-dir}")
    private String transactionsInQueueDir;

    @Value("${paths.transactions-in-queue-rest-dir}")
    private String transactionsInQueueRestDir;

    @Value("${paths.transactions-failed-rest-dir}")
    private String transactionsFailedRestDir;

    @Value("${paths.transactions-in-queue-big-files-dir}")
    private String transactionsInQueueBigFilesDir;

    @Value("${paths.articles-dir}")
    private String articlesDir;

    @Value("${paths.articles-from-dir}")
    private String articlesFromDir;

    @Value("${paths.articles-rejected-dir}")
    private String articlesRejectedDir;

    @Value("${paths.articles-accepted-dir}")
    private String articlesAcceptedDir;

    @Value("${paths.articles-export-dir}")
    private String articlesExportDir;

    @Value("${paths.charities-export-dir}")
    private String charitiesExportDir;

    @Value("${paths.oauth-export-dir}")
    private String oAuthClientsExportDir;

    @Value("${paths.companies-export-dir}")
    private String companiesExportDir;

    @Value("${paths.articles-pricat-dir}")
    private String articlesPricatExportDir;

    @Value("${paths.existing-bags-export-dir}")
    private String existingBagsExportDir;

    @Value("${paths.all-existing-bags-export-dir}")
    private String allExistingBagsExportDir;

    @Value("${paths.existing-transactions-export-dir}")
    private String existingTransactionsExportDir;

    @Value("${paths.all-existing-transactions-export-dir}")
    private String allExistingTransactionsExportDir;

    @Value("${paths.bags-dir}")
    private String bagsDir;

    @Value("${paths.bags-backup-dir}")
    private String bagsBackupDir;

    @Value("${paths.bags-accepted-dir}")
    private String bagsAcceptedDir;

    @Value("${paths.bags-rejected-dir}")
    private String bagsRejectedDir;

    @Value("${paths.bags-failed-dir}")
    private String bagsFailedDir;

    @Value("${paths.bags-confirmed-dir}")
    private String bagsConfirmedDir;

    @Value("${paths.bags-already-exists-dir}")
    private String bagsAlreadyExistsDir;

    @Value("${paths.bags-in-queue-dir}")
    private String bagsInQueueDir;

    @Value("${paths.bags-in-queue-big-files-dir}")
    private String bagsInQueueBigFilesDir;

    @Value("${paths.importer-rule-export-dir}")
    private String importerRuleExportDir;

    @Value("${paths.log-dir}")
    private String logDir;

    @Value("${paths.transaction-numbers-export-dir}")
    private String transactionNumbersExportDir;

    @Value("${paths.label-orders-dir}")
    private String labelOrdersDir;

    @Value("${paths.rejected-transactions-dir}")
    private String rejectedTransactionsDir;

    @Value("${paths.rejected-transactions-to-be-removed-dir}")
    private String rejectedTransactionsToBeRemovedDir;

    public DirectoryServiceImpl(BaseDirectoryRepository<BaseDirectory> directoryRepository,
                                BaseDocumentRepository<BaseDocument, BaseDirectory> documentRepository,
                                BaseSettingsService<BaseSettings> settingsService,
                                BaseEventLogService eventLogService,
                                Finder finder,
                                BaseAuthUtils authUtils) {
        super(directoryRepository, documentRepository, settingsService, eventLogService);
        this.finder = finder;
        this.authUtils = authUtils;
    }

    @Override
    public Path getRvmPath() {
        return getRoot().resolve(rvmDir);
    }

    @Override
    public Path getTransactionsPath() {
        return getRvmPath().resolve(transactionsDir);
    }

    @Override
    public Path getTransactionsFromPath() {
        return getTransactionsPath().resolve(transactionsFromDir);
    }

    @Override
    public Path getTransactionsBackupPath() {
        return getTransactionsPath().resolve(transactionsBackupDir);
    }

    @Override
    public Path getTransactionsRejectedPath() {
        return getTransactionsPath().resolve(transactionsRejectedDir);
    }

    @Override
    public Path getTransactionsFailedPath() {
        return getTransactionsPath().resolve(transactionsFailedDir);
    }

    @Override
    public Path getTransactionsAcceptedPath() {
        return getTransactionsPath().resolve(transactionsAcceptedDir);
    }

    @Override
    public Path getTransactionsConfirmedPath() {
        return getTransactionsPath().resolve(transactionsConfirmedDir);
    }

    @Override
    public Path getTransactionsAlreadyExistsPath() {
        return getTransactionsPath().resolve(transactionsAlreadyExistsDir);
    }

    @Override
    public Path getTransactionsInQueuePath() {
        return getTransactionsPath().resolve(transactionsInQueueDir);
    }

    @Override
    public Path getTransactionsInQueueRestPath() {
        return getTransactionsPath().resolve(transactionsInQueueRestDir);
    }

    @Override
    public Path getTransactionsFailedRestPath() {
        return getTransactionsPath().resolve(transactionsFailedRestDir);
    }

    @Override
    public Path getTransactionsInQueueBigFilesPath() {
        return getTransactionsPath().resolve(transactionsInQueueBigFilesDir);
    }

    @Override
    public Path getArticlesPath() {
        return getRvmPath().resolve(articlesDir);
    }

    @Override
    public Path getArticlesFromPath() {
        return getArticlesPath().resolve(articlesFromDir);
    }

    @Override
    public Path getArticlesRejectedPath() {
        return getArticlesPath().resolve(articlesRejectedDir);
    }

    @Override
    public Path getArticlesAcceptedPath() {
        return getArticlesPath().resolve(articlesAcceptedDir);
    }

    @Override
    public Path getArticlesExportPath() {
        return getRvmPath().resolve(articlesExportDir);
    }

    @Override
    public Path getCharitiesExportPath() {
        return getRvmPath().resolve(charitiesExportDir);
    }

    @Override
    public Path getOAuthClientsExportPath() {
        return getRvmPath().resolve(oAuthClientsExportDir);
    }

    @Override
    public Path getCompaniesExportPath() {
        return getRvmPath().resolve(companiesExportDir);
    }

    @Override
    public Path getExistingBagsExportPath() {
        return getRvmPath().resolve(existingBagsExportDir);
    }

    @Override
    public Path getAllExistingBagsExportPath() {
        return getRvmPath().resolve(allExistingBagsExportDir);
    }

    @Override
    public Path getExistingTransactionsExportPath() {
        return getRvmPath().resolve(existingTransactionsExportDir);
    }

    @Override
    public Path getAllExistingTransactionsExportPath() {
        return getRvmPath().resolve(allExistingTransactionsExportDir);
    }

    @Override
    public Path getArticlesPricatExportPath() {
        return getRvmPath().resolve(articlesPricatExportDir);
    }

    @Override
    public Path getBagsPath() {
        return getRvmPath().resolve(bagsDir);
    }

    @Override
    public Path getBagsBackupPath() {
        return getBagsPath().resolve(bagsBackupDir);
    }

    @Override
    public Path getBagsAcceptedPath() {
        return getBagsPath().resolve(bagsAcceptedDir);
    }

    @Override
    public Path getBagsRejectedPath() {
        return getBagsPath().resolve(bagsRejectedDir);
    }

    @Override
    public Path getBagsFailedPath() {
        return getBagsPath().resolve(bagsFailedDir);
    }

    @Override
    public Path getBagsConfirmedPath() {
        return getBagsPath().resolve(bagsConfirmedDir);
    }

    @Override
    public Path getBagsAlreadyExistsPath() {
        return getBagsPath().resolve(bagsAlreadyExistsDir);
    }

    @Override
    public Path getBagsInQueuePath() {
        return getBagsPath().resolve(bagsInQueueDir);
    }

    @Override
    public Path getBagsInQueueBigFilesPath() {
        return getBagsPath().resolve(bagsInQueueBigFilesDir);
    }

    @Override
    public Path getImporterRuleExportPath() {
        return getRvmPath().resolve(importerRuleExportDir);
    }

    @Override
    public Path getLogPath() {
        return getRvmPath().resolve(logDir);
    }

    @Override
    public Path getTransactionLogPath() {
        return getLogPath().resolve(transactionsDir);
    }

    @Override
    public Path getBagLogPath() {
        return getLogPath().resolve(bagsDir);
    }

    @Override
    public Path getArticlesLogPath() {
        return getLogPath().resolve(articlesDir);
    }

    @Override
    public Path getCompaniesLogPath() {
        return getLogPath().resolve(companiesExportDir);
    }

    @Override
    public Path getArticlePricatLogPath() {
        return getLogPath().resolve(articlesPricatExportDir);
    }

    @Override
    public Path getOAuthClientsLogPath() {
        return getLogPath().resolve(oAuthClientsExportDir);
    }

    @Override
    public Path getTransactionNumbersExportDir() {
        return getRvmPath().resolve(transactionNumbersExportDir);
    }

    @Override
    public Path getLabelOrdersPath() {
        return getRvmPath().resolve(labelOrdersDir);
    }

    @Override
    public Path getRejectedTransactionsPath() {
        return getRvmPath().resolve(rejectedTransactionsDir);
    }

    @Override
    public Path getRejectedTransactionsToBeRemovedDir() {
        return getRvmPath().resolve(rejectedTransactionsToBeRemovedDir);
    }

    @Override
    protected BaseDirectory findOld(BaseDirectory baseDirectory) {
        return finder.findOld(BaseDirectory.class, baseDirectory.getId());
    }

    @Override
    protected BaseDocument createDocument(String name, BaseDirectory parent) {
        BaseDocument document = new BaseDocument();
        document.setName(name);
        document.setDirectory(parent);
        document.setUser(authUtils.getCurrentUser());
        document = documentRepository.save(document);

        eventLogService.createAdd(document);
        return document;
    }

    @Override
    protected BaseDirectory createDirectory(String name, BaseDirectory parent, Set<String> roles, boolean isLog) {
        BaseDirectory directory = new BaseDirectory();
        directory.setName(name);
        directory.setParent(parent);
        directory.setRoles(new HashSet<>(roles));
        directory.setLogDirectory(isLog);
        directory.setUser(authUtils.getCurrentUser());
        directory = directoryRepository.save(directory);

        eventLogService.createAdd(directory);
        return directory;
    }

    @Override
    protected Path getRootDir(BaseDocument baseDocument) {
        return getRootDir(baseDocument.getDirectory());
    }
}
