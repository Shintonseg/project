package com.tible.ocm.services;

import com.tible.hawk.core.models.BaseDirectory;
import com.tible.hawk.core.models.BaseDocument;
import com.tible.hawk.core.services.BaseDirectoryService;

import java.nio.file.Path;

public interface DirectoryService extends BaseDirectoryService<BaseDirectory, BaseDocument> {
    Path getRvmPath();

    Path getTransactionsPath();

    Path getTransactionsFromPath();

    Path getTransactionsBackupPath();

    Path getTransactionsRejectedPath();

    Path getTransactionsFailedPath();

    Path getTransactionsAcceptedPath();

    Path getTransactionsConfirmedPath();

    Path getTransactionsAlreadyExistsPath();

    Path getTransactionsInQueuePath();

    Path getTransactionsInQueueRestPath();

    Path getTransactionsFailedRestPath();

    Path getTransactionsInQueueBigFilesPath();

    Path getArticlesPath();

    Path getArticlesFromPath();

    Path getArticlesRejectedPath();

    Path getArticlesAcceptedPath();

    Path getArticlesExportPath();

    Path getCharitiesExportPath();

    Path getOAuthClientsExportPath();

    Path getCompaniesExportPath();

    Path getArticlesPricatExportPath();

    Path getExistingBagsExportPath();

    Path getAllExistingBagsExportPath();

    Path getExistingTransactionsExportPath();
    
    Path getAllExistingTransactionsExportPath();

    Path getBagsPath();

    Path getBagsBackupPath();

    Path getBagsRejectedPath();

    Path getBagsFailedPath();

    Path getBagsConfirmedPath();

    Path getBagsAcceptedPath();

    Path getBagsAlreadyExistsPath();

    Path getBagsInQueuePath();

    Path getBagsInQueueBigFilesPath();

    Path getImporterRuleExportPath();

    Path getLogPath();

    Path getTransactionLogPath();

    Path getBagLogPath();

    Path getArticlesLogPath();

    Path getCompaniesLogPath();

    Path getArticlePricatLogPath();

    Path getOAuthClientsLogPath();

    Path getTransactionNumbersExportDir();

    Path getLabelOrdersPath();

    Path getRejectedTransactionsPath();

    Path getRejectedTransactionsToBeRemovedDir();
}
