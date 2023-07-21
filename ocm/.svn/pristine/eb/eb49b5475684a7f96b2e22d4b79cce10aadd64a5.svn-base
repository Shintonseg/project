package com.tible.ocm.services;

import com.tible.ocm.models.mongo.Company;

import java.nio.file.Path;

public interface AAFilesService {

    void processAABagFiles(Company company, Path file, boolean moveFailedToCompanyRejectedDirectory);

    void processAATransactionFiles(Company company, Path file, boolean moveFailedToCompanyRejectedDirectory);

    void processAABackupOrFailedFiles(Company company, Path readyPath, boolean failedDir);
}
