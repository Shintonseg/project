package com.tible.ocm.services.impl;

import com.tible.hawk.core.controllers.helpers.MailData;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.hawk.core.utils.ImportType;
import com.tible.ocm.dto.log.LogFileInfo;
import com.tible.ocm.models.CsvRecordType;
import com.tible.ocm.models.ImportMessage;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.ImporterRule;
import com.tible.ocm.models.mongo.RefundArticle;
import com.tible.ocm.repositories.mongo.CompanyRepository;
import com.tible.ocm.repositories.mongo.RefundArticleRepository;
import com.tible.ocm.services.ArticleService;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.ImporterRuleService;
import com.tible.ocm.services.log.LogExporterService;
import com.tible.ocm.utils.DateUtils;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tible.hawk.core.utils.ExportHelper.writeValues;
import static com.tible.hawk.core.utils.ImportHelper.readFileWithLineNumber;
import static com.tible.ocm.models.CsvRecordType.POS;
import static com.tible.ocm.services.log.LogKeyConstant.*;
import static com.tible.ocm.utils.ImportHelper.*;

@Slf4j
@Primary
@Service
public class ArticleServiceImpl implements ArticleService {

    @Value("#{'${mail-to.file-import-failed}'.split(',')}")
    private List<String> fileImportFailedMailTo;

    private final DirectoryService directoryService;
    private final BaseMailService mailService;
    private final ImporterRuleService importerRuleService;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final RefundArticleRepository refundArticleRepository;
    private final LogExporterService<LogFileInfo> loggerExporterService;

    @Autowired
    public ArticleServiceImpl(DirectoryService directoryService,
                              BaseMailService mailService,
                              ImporterRuleService importerRuleService,
                              CompanyRepository companyRepository,
                              CompanyService companyService,
                              RefundArticleRepository refundArticleRepository,
                              LogExporterService<LogFileInfo> loggerExporterService) {
        this.directoryService = directoryService;
        this.mailService = mailService;
        this.importerRuleService = importerRuleService;
        this.companyRepository = companyRepository;
        this.companyService = companyService;
        this.refundArticleRepository = refundArticleRepository;
        this.loggerExporterService = loggerExporterService;
    }

    @Override
    public void processArticleFile(String number, String version, List<RefundArticle> refundArticles, String ipAddress,
                                   Path file, boolean moveFailedToCompanyRejectedDirectory, String communication) {
        final Path rejected = directoryService.getArticlesRejectedPath();
        final Path rejectedCompany = rejected.resolve(number);
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(rejected)) {
            log.error("Creating rejected directory failed");
        }

        final Path accepted = directoryService.getArticlesAcceptedPath();
        if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(accepted)) {
            log.error("Creating accepted directory failed");
        }

        final Path parent = file.getParent();
        final Path hashFile = parent.resolve(getFilename(file) + ".hash");

        LogFileInfo logFileInfo = LogFileInfo.builder()
                .fileName(getFilename(file))
                .isNeedExport(true)
                .path(directoryService.getArticlesLogPath()).build();

        Map<String, Object> contentLog = new HashMap<>(Map.of(COMMUNICATION_KEY, communication));

        try {
            if (!Files.exists(hashFile) || !FileUtils.compareSha256HexFromHashFile(file, hashFile)) {
                log.warn("Moving {} articles files to rejected folder, because hash is wrong or missing", file.toString());
                if (Files.exists(hashFile)) {
                    log.warn("File hash is: {}", FileUtils.getSha256HexFromFile(file));
                }

                processFile(moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, ipAddress);
                contentLog.put(DETAILS_KEY, String.format("Moving %s articles files to rejected folder, because hash is wrong or missing", file));
                loggerExporterService.logToFile(logFileInfo, contentLog);
                return;
            }

            List<ImportMessage> failedChecks = checkMissingImportTypes(file, false);

            updateCsvFile(file.toFile(), refundArticles);

            failedChecks.addAll(readAndCheckArticleFile(number, version, refundArticles, file));
            if (!failedChecks.isEmpty()) {
                MailData mailData = createMail("OCM one or more of the import checks for file " + file.getFileName().toString() + " failed",
                        file.getFileName().toString(), failedChecks, fileImportFailedMailTo);
                mailService.sendMail(mailData, null);
                contentLog.put(DETAILS_KEY, String.format("Moving articles files %s to rejected folder, because one or more import checks failed", file));
                contentLog.put(IMPORT_MESSAGES_KEY, failedChecks);
                loggerExporterService.logToFile(logFileInfo, contentLog);

                log.warn("Moving {} articles files to rejected folder, because one or more import checks failed", file);
                processFile(moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, ipAddress);
                return;
            }
            updateCsvFileWithImporterRules(file.toFile(), companyRepository.findByNumber(number));
            String newFileName = getFilename(file) + "-" + number + ".csv";
            String newHashFileName = getFilename(hashFile) + "-" + number + ".hash";
            moveAndRenameIfExists(accepted, file, newFileName);
            Files.deleteIfExists(hashFile);
            createArticleHashFile(accepted.resolve(newFileName), accepted.resolve(newHashFileName));

            contentLog.put(DETAILS_KEY, "Article file " + file + " was handled successfully");
            contentLog.put(DIRECTORY_KEY, ipAddress);
            loggerExporterService.logToFile(logFileInfo, contentLog);

        } catch (Exception e) {
            log.warn("Moving {} articles files to rejected folder", file, e);
            processFile(moveFailedToCompanyRejectedDirectory, rejectedCompany, file, hashFile, ipAddress);
            contentLog.put(DETAILS_KEY, String.format("Moving %s articles files to rejected folder", file));
            loggerExporterService.logToFile(logFileInfo, contentLog);
        }

    }

    private void processFile(boolean moveFailedToCompanyRejectedDirectory, Path rejectedCompany, Path file, Path hashFile, String ipAddress) {
        if (moveFailedToCompanyRejectedDirectory) {
            copyIfExists(rejectedCompany, file, hashFile);
            moveToRejectedCompanyDirectory(file, hashFile, ipAddress);
        } else {
            moveIfExists(rejectedCompany, file, hashFile);
        }
    }

    private void moveToRejectedCompanyDirectory(Path file, Path hashFile, String ipAddress) {
        Path companyPath = directoryService.getRoot().resolve(ipAddress);
        Path companyTransRejectedPath = companyPath.resolve(OUTPUT_DIRECTORY).resolve(REJECTED_DIRECTORY);
        moveIfExists(companyTransRejectedPath, file, hashFile);
    }

    private List<ImportMessage> readAndCheckArticleFile(String number, String version, List<RefundArticle> refundArticles, Path file) throws IOException {
        List<ImportMessage> failedChecks = new ArrayList<>();
        AtomicInteger posCount = new AtomicInteger(0);
        readFileWithLineNumber(file, (scanner, importType) -> {
            switch (CsvRecordType.valueOf(importType.getType())) {
                case HDR:
                    failedChecks.addAll(checkArticleHeader(number, version, scanner, importType));
                    break;
                case POS:
                    failedChecks.addAll(checkArticlePos(refundArticles, scanner, importType));
                    posCount.getAndAdd(1);
                    break;
                case SUM:
                    failedChecks.addAll(checkArticleSum(number, version, scanner, importType, posCount));
                    break;
            }
        });
        return failedChecks;
    }

    private List<ImportMessage> checkArticleHeader(String number, String version, Scanner scanner, ImportType importType) {
        Company company = companyService.findByNumber(number);
        int dataExpirationPeriodInDays = companyService.getDataExpirationPeriodInDays(company);
        return checkFileHeader(number, version, scanner, importType, dataExpirationPeriodInDays);
    }

    private List<ImportMessage> checkArticlePos(List<RefundArticle> refundArticles, Scanner scanner, ImportType importType) {
        List<ImportMessage> failedChecks = new ArrayList<>();
        String ean = scanner.next(); // article number
        if (StringUtils.isEmpty(ean)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "EAN field is empty"));
        }
        String supplier = scanner.next(); // supplier
        if (StringUtils.isEmpty(supplier)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Supplier field is empty"));
        }
        String activationDate = scanner.next(); // activation date
        String weightMin = scanner.next(); // weight min
        if (StringUtils.isEmpty(weightMin)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Weight min field is empty"));
        }
        String weightMax = scanner.next(); // weight max
        if (StringUtils.isEmpty(weightMax)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Weight max field is empty"));
        }
        if (Integer.parseInt(weightMin) > Integer.parseInt(weightMax)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Weight min field is bigger than weight max field"));
        }
        String volume = scanner.next(); // volume
        if (StringUtils.isEmpty(volume)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Volume field is empty"));
        }
        String articleType = scanner.next(); // article type
        if (StringUtils.isEmpty(articleType)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Article type field is empty"));
        } else {
            int articleTypeInt = Integer.parseInt(articleType);
            if (articleTypeInt < 0 || articleTypeInt > 1) {
                failedChecks.add(new ImportMessage(importType.getLineNumber(),
                        "Article type field is " + articleTypeInt + ", expected: 0 or 1"));
            }
        }
        String description = scanner.next(); // volume
        if (StringUtils.isEmpty(description)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "Description field is empty"));
        }

        return failedChecks;
    }

    private List<ImportMessage> checkArticleSum(String number, String version, Scanner scanner, ImportType importType, AtomicInteger posCount) {
        List<ImportMessage> failedChecks = checkFileSum(number, version, scanner, importType, posCount);
        String rvmWildcardAmount = scanner.next(); // rvm wildcard amount
        if (StringUtils.isEmpty(rvmWildcardAmount)) {
            failedChecks.add(new ImportMessage(importType.getLineNumber(),
                    "RVM article wildcard amount field is empty"));
        }
        return failedChecks;
    }

    private void createArticleHashFile(Path articleFile, Path articleHashFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(articleHashFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writeValues(writer, false, FileUtils.getSha256HexFromFile(articleFile));
        }
    }

    /**
     * Filling csv with actual data from refundArticles.
     *
     * @param file           file
     * @param refundArticles refundArticles
     * @throws Exception exception thrown
     */
    public static void updateCsvFile(File file, List<RefundArticle> refundArticles) throws Exception {
        try (CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.DEFAULT.withDelimiter(';'))) {
            List<CSVRecord> list = parser.getRecords();
            String edited = file.getAbsolutePath();
            file.delete();
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(edited),
                    CSVFormat.DEFAULT.withDelimiter(';').withQuoteMode(QuoteMode.ALL))) {
                for (CSVRecord record : list) {
                    fillCsvRecord(record, refundArticles, printer);
                }
            }
        }
    }

    public void updateCsvFileWithImporterRules(File file, Company company) throws IOException {
        List<ImporterRule> importerRules = importerRuleService.getAllByRvmOwnerAndRvmSerial(company.getRvmOwnerNumber(), company.getSerialNumbers());

        try (CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.DEFAULT.withDelimiter(';'))) {
            List<CSVRecord> list = parser.getRecords();
            String edited = file.getAbsolutePath();

            file.delete();
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(edited),
                    CSVFormat.DEFAULT.withDelimiter(';').withQuoteMode(QuoteMode.ALL))) {
                for (CSVRecord record : list) {

                    if (!record.get(0).equals(POS.name())) {
                        printer.printRecord(record);
                        return;
                    }

                    String ean = record.get(1);
                    Optional<ImporterRule> article = importerRules.stream()
                            .filter(it -> it.getFromEan().equals(ean))
                            .findFirst();


                    if (article.isPresent()) {
                        ImporterRule rule = article.get();
                        Optional<RefundArticle> refundArticleTo = refundArticleRepository.findByNumber(rule.getToEan());
                        if (refundArticleTo.isPresent()) {
                            printer.printRecord(POS, ean, refundArticleTo.get().getSupplier(), DateUtils.fillBasicIsoDate(refundArticleTo.get().getActivationDate().toLocalDate()),
                                    refundArticleTo.get().getWeightMin(), refundArticleTo.get().getWeightMax(), refundArticleTo.get().getVolume(), refundArticleTo.get().getType(), refundArticleTo.get().getDescription());
                        }
                    } else {
                        printer.printRecord(record);
                    }
                }
            }
        }
    }

    private static void fillCsvRecord(CSVRecord record, List<RefundArticle> refundArticles, CSVPrinter printer) throws
            IOException {

        if (!record.get(0).equals(POS.name())) {
            printer.printRecord(record);
            return;
        }

        String ean = record.get(1);
        // String supplier = record.get(2);
        // String activationDate = record.get(3);
        // String weightMin = record.get(4);
        // String weightMax = record.get(5);
        // String volume = record.get(6);
        // String articleType = record.get(7);
        // String description = record.get(8);

        Optional<RefundArticle> article = refundArticles.stream()
                .filter(it -> it.getNumber().equals(ean))
                .findFirst();

        if (article.isPresent()) {
            RefundArticle ra = article.get();
            printer.printRecord(POS, ean, ra.getSupplier(), DateUtils.fillBasicIsoDate(ra.getActivationDate().toLocalDate()),
                    ra.getWeightMin(), ra.getWeightMax(), ra.getVolume(), ra.getType(), ra.getDescription());
        } else {
            printer.printRecord(record);
        }

    }
}
