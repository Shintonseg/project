package com.tible.ocm.rabbitmq;

import com.tible.hawk.core.utils.FileUtils;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.AAFilesService;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.RvmTransactionService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
public class TransactionImportService {

    private final DirectoryService directoryService;
    private final RvmTransactionService rvmTransactionService;
    private final CompanyService companyService;
    private final AAFilesService aaFilesService;


    public TransactionImportService(DirectoryService directoryService, RvmTransactionService rvmTransactionService, CompanyService companyService, AAFilesService aaFilesService) {
        this.directoryService = directoryService;
        this.rvmTransactionService = rvmTransactionService;
        this.companyService = companyService;
        this.aaFilesService = aaFilesService;
    }

    public void handleTransactionFilePayload(TransactionFilePayload payload, boolean bigFiles) {
        final Path bagsInQueueDir = bigFiles ? directoryService.getBagsInQueueBigFilesPath() : directoryService.getBagsInQueuePath();
        if (!FileUtils.checkOrCreateDir(bagsInQueueDir)) {
            log.error("Creating bag in queue directory failed");
        }

        final Path transactionsInQueueDir = bigFiles ? directoryService.getTransactionsInQueueBigFilesPath() : directoryService.getTransactionsInQueuePath();
        if (!FileUtils.checkOrCreateDir(transactionsInQueueDir)) {
            log.error("Creating transaction in queue directory failed");
        }

        Optional<Company> companyOptional = companyService.findById(payload.getCompanyId());
        companyOptional.ifPresentOrElse(company -> {
            if (payload.getType().equals(ImportHelper.FILE_TYPE_AA_BAG)) {
                Path inQueueCompany = bagsInQueueDir.resolve(company.getIpAddress());
                OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);
                Path inQueueFilePath = inQueueCompany.resolve(payload.getName());
                if (Files.exists(inQueueFilePath)) {
                    // BagTransaction bagTransaction = bagTransactionService.processBagTransactionFile(inQueuePath, false);
                    aaFilesService.processAABagFiles(company, inQueueFilePath, true);
                    log.info("Processed AA bag from queue {} with big files as {}", payload.getName(), bigFiles);
                } else {
                    log.info("Already processed AA bag from queue {} with big files as {}", payload.getName(), bigFiles);
                }
            } else if (payload.getType().equals(ImportHelper.FILE_TYPE_AA_TRANSACTION)) {
                Path inQueueCompany = transactionsInQueueDir.resolve(company.getIpAddress());
                OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);
                Path inQueueFilePath = inQueueCompany.resolve(payload.getName());
                if (Files.exists(inQueueFilePath)) {
                    // Transaction transaction = transactionService.processTransactionFile(inQueuePath, false);
                    aaFilesService.processAATransactionFiles(company, inQueueFilePath, true);
                    log.info("Processed AA transaction from queue {} with big files as {}", payload.getName(), bigFiles);
                } else {
                    log.info("Already processed AA transaction from queue {} with big files as {}", payload.getName(), bigFiles);
                }
            } else {
                Path inQueueCompany = transactionsInQueueDir.resolve(company.getIpAddress());
                OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);
                Path inQueueFilePath = inQueueCompany.resolve(payload.getName());
                if (Files.exists(inQueueFilePath)) {
                    // Transaction transaction = transactionService.processTransactionFile(inQueuePath, false);
                    rvmTransactionService.processTransactionFile(company, inQueueFilePath, true, true);
                    log.info("Processed transaction from queue {}", payload.getName());
                } else {
                    log.info("Already processed transaction from queue {}", payload.getName());
                }
            }
        }, () -> {
            log.error("Could not find company id {}", payload.getCompanyId());
        });
    }
}
