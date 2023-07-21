package com.tible.ocm.services;

import com.tible.ocm.models.mongo.Company;

import java.nio.file.Path;

public interface RvmTransactionService {

    void processTransactionFile(Company company, Path path,
                                boolean moveFailedToCompanyRejectedDirectory, boolean saveTransaction);

    void backupTransactionFile(String number, String version, Path file);

    void processTransactionBackupOrFailedFiles(Company company, Path csvPath, boolean failedDir);
}
