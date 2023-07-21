package com.tible.ocm.acceptance.utils;

import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.dto.file.FileContent;
import com.tible.ocm.dto.file.TransactionBody;
import com.tible.ocm.dto.file.TransactionHeader;
import com.tible.ocm.models.CommunicationType;
import com.tible.ocm.models.CompanyType;
import com.tible.ocm.models.CsvRecordType;
import com.tible.ocm.models.OcmVersion;
import com.tible.ocm.models.mongo.*;
import com.tible.ocm.models.mysql.ExistingBag;
import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.repositories.mongo.*;
import com.tible.ocm.repositories.mysql.ExistingBagRepository;
import com.tible.ocm.repositories.mysql.ExistingTransactionRepository;
import com.tible.ocm.services.SrnRemovedArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static com.tible.hawk.core.utils.ImportHelper.readFileWithLineNumber;
import static com.tible.ocm.acceptance.utils.TestCase.*;
import static com.tible.ocm.models.CommunicationType.SFTP;
import static com.tible.ocm.utils.ImportHelper.*;
import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;

@Component
public class TransactionFileParser {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionFileParser.class);
    private static final String ROOT_DIR = "testSync";
    private static final String RVM_OWNER_NUMBER = "222";
    private static final String IP_RANGE = "192.168.85.0/24";
    private static final String LOCALIZATION_NUMBER = "1111";
    private static final Path PATH_TO_RVM_TRANSACTIONS_DIRECTORY = Path.of(ROOT_DIR + "/RVM/transactions");
    private static final String CSV_EXTENSION = ".csv";
    private static final String WILDCARD_EAN_FROM = "0000003050010";
    private static final String UNKNOWN_EAN = "1234567891234";
    private static final String NOT_ACTIVATED_EAN = "45645433";
    private static final String TO_EAN = "3050010";

    private final CompanyRepository companyRepository;
    private final LabelOrderRepository labelOrderRepository;
    private final SrnArticleRepository srnArticleRepository;
    private final TransactionRepository transactionRepository;
    private final ExistingTransactionRepository existingTransactionRepository;
    private final ImporterRuleRepository importerRuleRepository;
    private final ImporterRuleLimitationsRepository importerRuleLimitationsRepository;
    private final ExistingBagRepository existingBagRepository;
    private final SrnRemovedArticleService srnRemovedArticleService;
    private final TransactionArticleRepository transactionArticleRepository;

    public TransactionFileParser(CompanyRepository companyRepository,
                                 LabelOrderRepository labelOrderRepository,
                                 SrnArticleRepository srnArticleRepository,
                                 TransactionRepository transactionRepository,
                                 ExistingTransactionRepository existingTransactionRepository,
                                 ImporterRuleRepository importerRuleRepository,
                                 ImporterRuleLimitationsRepository importerRuleLimitationsRepository,
                                 ExistingBagRepository existingBagRepository,
                                 SrnRemovedArticleService srnRemovedArticleService,
                                 TransactionArticleRepository transactionArticleRepository) {
        this.companyRepository = companyRepository;
        this.labelOrderRepository = labelOrderRepository;
        this.srnArticleRepository = srnArticleRepository;
        this.transactionRepository = transactionRepository;
        this.existingTransactionRepository = existingTransactionRepository;
        this.importerRuleRepository = importerRuleRepository;
        this.importerRuleLimitationsRepository = importerRuleLimitationsRepository;
        this.existingBagRepository = existingBagRepository;
        this.srnRemovedArticleService = srnRemovedArticleService;
        this.transactionArticleRepository = transactionArticleRepository;
    }

    public void parseTestTransactionFile(Path filePath,
                                         String ipAddress,
                                         String companyNumber,
                                         String directoryName,
                                         Integer caseNumber) throws IOException {
        FileContent<TransactionHeader, TransactionBody> fileContent = new FileContent<>();
        ArrayList<TransactionBody> fileBody = new ArrayList<>();

        String fileName = getFilename(filePath);
        String version = getFileVersion(filePath);

        readFileWithLineNumber(filePath, ((scanner, importType) -> {
            switch (CsvRecordType.valueOf(importType.getType())) {
                case HDR:
                    TransactionHeader transactionHeader = TransactionHeader.header(scanner, version, importType);
                    fileContent.setHeader(transactionHeader);
                    break;
                case POS:
                    TransactionBody transactionBody = TransactionBody.body(scanner, version, importType);
                    fileBody.add(transactionBody);
                    break;
            }
        }));

        processTransactionFile(fileName, version, fileContent, fileBody, ipAddress, companyNumber, directoryName, caseNumber);
    }

    public void processTransactionDto(String ipAddress,
                                      String companyNumber,
                                      String testNumber,
                                      Integer caseNumber,
                                      TransactionDto transactionDto) {
        String version = transactionDto.getVersion();
        Company company = new Company();
        company.setIpAddress(ipAddress);
        company.setNumber(companyNumber);
        company.setCommunication(CommunicationType.REST);
        company.setSerialNumbers(List.of(transactionDto.getSerialNumber()));
        company.setRvmOwnerNumber(RVM_OWNER_NUMBER);
        company.setVersion(version);
        company.setStoreId(transactionDto.getStoreId());
        company.setType(CompanyType.DISTRIBUTION_CENTER.name());

        Company rvmOwner = new Company();
        rvmOwner.setNumber(RVM_OWNER_NUMBER);
        rvmOwner.setType(CompanyType.CUSTOMER.name());
        rvmOwner.setVersion(transactionDto.getVersion());
        rvmOwner.setStoreId("11111112");
        rvmOwner.setUsingIpTrunking(false);
        rvmOwner.setIpAddress(ipAddress);
        rvmOwner.setIpRange(IP_RANGE);
        rvmOwner.setCommunication(SFTP);
        rvmOwner.setSerialNumbers(List.of("88888882"));
        rvmOwner.setRvmOwnerNumber("009");
        rvmOwner.setAllowDataYoungerThanDays(3650);
        rvmOwner.setLocalizationNumber(LOCALIZATION_NUMBER);

        List<SrnArticle> srnArticles = transactionDto.getArticles().parallelStream().map(articleDto -> {
            SrnArticle article = new SrnArticle();
            article.setNumber(articleDto.getArticleNumber());
            article.setFirstArticleActivationDate(LocalDateTime.now());
            article.setWeight(20);
            article.setVolume(20);
            return article;
        }).collect(Collectors.toList());

        srnArticleRepository.saveAll(srnArticles);

        if (!SCENARIO_128.getNumber().equals(testNumber) && transactionDto.getNumber() != null && !transactionDto.getNumber().isEmpty()) {
            String customerNumberFromLabel = getCustomerNumberFromLabel(transactionDto.getNumber());
            int numberFromLabel = getLabelNumberFromLabel(transactionDto.getNumber());

            LabelOrder labelOrder = new LabelOrder();
            labelOrder.setCustomerNumber(customerNumberFromLabel);
            labelOrder.setRvmOwnerNumber(RVM_OWNER_NUMBER);
            labelOrder.setLastLabelNumber((long) (numberFromLabel + 1));
            labelOrder.setFirstLabelNumber((long) (numberFromLabel - 1));
            labelOrder.setMarkAllLabelsAsUsed(false);
            labelOrderRepository.save(labelOrder);
        }

        companyRepository.save(company);
        companyRepository.save(rvmOwner);

        givenForTests(transactionDto.getTransactionNumber(), testNumber, caseNumber, version, company, transactionDto.getSerialNumber(), transactionDto.getDateTime());
    }

    public void cleanUpDb() {
        companyRepository.deleteAll();
        srnArticleRepository.deleteAll();
        labelOrderRepository.deleteAll();
        transactionRepository.deleteAll();
        transactionArticleRepository.deleteAll();
        existingTransactionRepository.deleteAll();
        importerRuleRepository.deleteAll();
        importerRuleLimitationsRepository.deleteAll();
        existingBagRepository.deleteAll();
    }

    private String getFileVersion(Path filePath) {
        String version = null;
        try (LineNumberReader reader = new LineNumberReader(Files.newBufferedReader(filePath, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line != null) {
                try (Scanner scanner = new Scanner(line)) {
                    scanner.useDelimiter(";|(\r)?\n");
                    scanner.next();
                    version = scanner.next().replace("\"", "");
                }
            }
        } catch (Exception e) {
            LOG.info("Cant read version from file");
            return null;
        }
        return version;
    }

    private void processTransactionFile(String fileName,
                                        String version,
                                        FileContent<TransactionHeader, TransactionBody> fileContent,
                                        ArrayList<TransactionBody> fileBody,
                                        String ipAddress,
                                        String companyNumber,
                                        String testNumber,
                                        Integer caseNumber) {
        TransactionHeader header = fileContent.getHeader();

        Company firstCompany = new Company();
        firstCompany.setStoreId(header.getStoreId());
        firstCompany.setType(CompanyType.CUSTOMER.name());
        firstCompany.setNumber(companyNumber);
        firstCompany.setVersion(version);
        firstCompany.setCommunication(CommunicationType.SFTP);
        firstCompany.setIpAddress(ipAddress);
        firstCompany.setRvmOwnerNumber(RVM_OWNER_NUMBER);
        firstCompany.setAllowDataYoungerThanDays(1000);
        firstCompany.setLocalizationNumber(LOCALIZATION_NUMBER);
        firstCompany.setSerialNumbers(List.of(String.valueOf(Integer.parseInt(header.getRvmSerial()))));

        Company rvmOwner = new Company();
        rvmOwner.setNumber(RVM_OWNER_NUMBER);
        rvmOwner.setType(CompanyType.CUSTOMER.name());
        rvmOwner.setAllowDataYoungerThanDays(1000);

        companyRepository.save(firstCompany);
        companyRepository.save(rvmOwner);

        if (!SCENARIO_128.getNumber().equals(testNumber) && header.getLabelNumber() != null && !header.getLabelNumber().isEmpty()) {
            String customerNumberFromLabel = getCustomerNumberFromLabel(header.getLabelNumber());
            int numberFromLabel = getLabelNumberFromLabel(header.getLabelNumber());

            LabelOrder labelOrder = new LabelOrder();
            labelOrder.setCustomerNumber(customerNumberFromLabel);
            labelOrder.setRvmOwnerNumber(RVM_OWNER_NUMBER);
            labelOrder.setLastLabelNumber((long) (numberFromLabel + 1));
            labelOrder.setFirstLabelNumber((long) (numberFromLabel - 1));
            labelOrder.setMarkAllLabelsAsUsed(false);
            labelOrderRepository.save(labelOrder);
        }

        List<SrnArticle> srnArticles = fileBody.stream().parallel().map(body -> {
            SrnArticle article = new SrnArticle();
            article.setNumber(body.getArticleNumber());
            article.setWeight(20);
            article.setVolume(20);
            article.setMaterial(1);
            return article;
        }).collect(Collectors.toList());

        srnArticleRepository.saveAll(srnArticles);

        givenForTests(fileName, testNumber, caseNumber, version, firstCompany, header.getRvmSerial(), LocalDateTime.parse(header.getDateTime(), DATETIMEFORMATTER));
    }

    public void parseTestAATransactionFile(Path filePath,
                                           String ipAddress,
                                           String companyNumber,
                                           String directoryName,
                                           Integer caseNumber) throws IOException {

        String fileName = getFilename(filePath);

        processAATransactionFile(fileName, ipAddress, companyNumber, directoryName, caseNumber);
    }

    private void processAATransactionFile(String fileName,
                                          String ipAddress,
                                          String companyNumber,
                                          String testNumber,
                                          Integer caseNumber) {

        Company firstCompany = new Company();
        firstCompany.setStoreId("1");
        firstCompany.setType(CompanyType.CUSTOMER.name());
        firstCompany.setNumber(companyNumber);
        firstCompany.setVersion("017");
        firstCompany.setCommunication(CommunicationType.AA_TRANSACTION);
        firstCompany.setIpAddress(ipAddress);
        firstCompany.setRvmOwnerNumber(RVM_OWNER_NUMBER);
        firstCompany.setAllowDataYoungerThanDays(1000);
        firstCompany.setLocalizationNumber(LOCALIZATION_NUMBER);

        Company rvmOwner = new Company();
        rvmOwner.setNumber(RVM_OWNER_NUMBER);
        rvmOwner.setType(CompanyType.CUSTOMER.name());
        rvmOwner.setAllowDataYoungerThanDays(1000);

        companyRepository.save(firstCompany);
        companyRepository.save(rvmOwner);
    }

    private void givenForTests(String fileName,
                               String testNumber,
                               Integer caseNumber,
                               String version,
                               Company company,
                               String rvmSerial,
                               LocalDateTime dateTime) {

        if (SCENARIO_101.getNumber().equals(testNumber)) {
            company.setSerialNumbers(List.of(String.valueOf(Integer.parseInt(rvmSerial) - 1)));
            companyRepository.save(company);
        }

        if (SCENARIO_103.getNumber().equals(testNumber)) {
            if (caseNumber == 1) {
                Transaction existingTransaction = new Transaction();
                existingTransaction.setTransactionNumber(fileName);
                transactionRepository.save(existingTransaction);
            } else if (caseNumber == 2) {
                ExistingTransaction existingTransaction = new ExistingTransaction();
                existingTransaction.setNumber(fileName);
                existingTransaction.setRvmOwnerNumber(RVM_OWNER_NUMBER);
                existingTransactionRepository.save(existingTransaction);
            } else if (caseNumber == 3) {
                addTransactionFileToAcceptedDir(testNumber, version, fileName, company);
            }
        }

        if (SCENARIO_104.getNumber().equals(testNumber)) {//delete wildcard ean from db, but save toEan
            SrnArticle srnArticle = srnArticleRepository.findByNumber(WILDCARD_EAN_FROM);
            srnArticleRepository.delete(srnArticle);
            SrnArticle articleTo = new SrnArticle();
            articleTo.setNumber(TO_EAN);
            articleTo.setWeight(1);
            articleTo.setFirstArticleActivationDate(LocalDateTime.now());
            srnArticleRepository.save(articleTo);

            ImporterRule importerRule = new ImporterRule();
            importerRule.setFromEan(WILDCARD_EAN_FROM);
            importerRule.setToEan(TO_EAN);
            importerRuleRepository.save(importerRule);

            ImporterRuleLimitations limitations = new ImporterRuleLimitations();
            limitations.setImporterRuleId(importerRule.getId());
            limitations.setRvmOwner(company.getRvmOwnerNumber());
            limitations.setRvmSerials(List.of(rvmSerial));

            importerRuleLimitationsRepository.save(limitations);
        }

        if (SCENARIO_105.getNumber().equals(testNumber)) {
            SrnArticle srnArticle = srnArticleRepository.findByNumber(WILDCARD_EAN_FROM);
            srnArticleRepository.delete(srnArticle);
        }

        if (SCENARIO_106.getNumber().equals(testNumber)) {
            SrnArticle srnArticle = srnArticleRepository.findByNumber(UNKNOWN_EAN);
            srnArticleRepository.delete(srnArticle);
        }

        if (SCENARIO_107.getNumber().equals(testNumber)) {
            if (caseNumber == 1) {
                SrnArticle srnArticle = srnArticleRepository.findByNumber(NOT_ACTIVATED_EAN);
                srnArticle.setActivationDate(dateTime.plusDays(10));
                srnArticle.setFirstArticleActivationDate(null);
                srnArticleRepository.save(srnArticle);
            } else if (caseNumber == 2) {
                SrnArticle srnArticle = srnArticleRepository.findByNumber(NOT_ACTIVATED_EAN);
                srnArticle.setActivationDate(dateTime.plusDays(20));
                srnArticle.setFirstArticleActivationDate(dateTime.plusDays(10));
                srnArticleRepository.save(srnArticle);
            }
        }

        if (SCENARIO_108.getNumber().equals(testNumber)) {
            company.setVersion(OcmVersion.VERSION_15.title);
            companyRepository.save(company);
        }

        if (SCENARIO_111.getNumber().equals(testNumber)) {
            if (caseNumber == 1) {// case with lower weight
                SrnArticle srnArticle = srnArticleRepository.findByNumber(TO_EAN);
                srnArticle.setWeight(251);
                srnArticleRepository.save(srnArticle);
            } else if (caseNumber == 2) {// case with higher weight
                SrnArticle srnArticle = srnArticleRepository.findByNumber(TO_EAN);
                srnArticle.setVolume(1);
                srnArticleRepository.save(srnArticle);
            }
        }

        if (SCENARIO_113.getNumber().equals(testNumber) ||
                SCENARIO_116.getNumber().equals(testNumber) ||
                SCENARIO_119.getNumber().equals(testNumber)) {
            SrnArticle srnArticle = srnArticleRepository.findByNumber(TO_EAN);
            srnArticle.setMaterial(2);
            srnArticleRepository.save(srnArticle);
        }

        if (SCENARIO_127.getNumber().equals(testNumber) || SCENARIO_129.getNumber().equals(testNumber)) {
            company.setCommunication(CommunicationType.AH_TOMRA);//need to set anything rather then rest
            companyRepository.save(company);
        }

        if (SCENARIO_129.getNumber().equals(testNumber)) {
            ExistingBag existingBag = new ExistingBag();
            existingBag.setCombinedCustomerNumberLabel("123450891");
            existingBagRepository.save(existingBag);
        }

        if (SCENARIO_130.getNumber().equals(testNumber)) {
            SrnRemovedArticle removedArticle = SrnRemovedArticle.builder()
                    .number(TO_EAN)
                    .deactivationDate(LocalDateTime.now().minusDays(3))
                    .build();
            srnRemovedArticleService.saveSrnRemovedArticles(Collections.singletonList(removedArticle));
        }

        if (SCENARIO_100_1.getNumber().equals(testNumber)) {
            Transaction existingTransaction = new Transaction();
            existingTransaction.setTransactionNumber(fileName);
            transactionRepository.save(existingTransaction);
        }
    }

    private void addTransactionFileToAcceptedDir(String testNumber, String version, String fileName, Company company) {
        try {
            OcmVersion ocmVersion = OcmVersion.valueOfTitle(version);
            if (ocmVersion != null) {
                Files.createDirectories(PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("accepted"));

                Path resourceDirectoryCsv = Paths.get("src", "test", "resources", "testFiles", "sftp", testNumber, ocmVersion.number.toString(), fileName + CSV_EXTENSION);
                Path testFileCsv = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("accepted").resolve(fileName + "-" + company.getNumber() + CSV_EXTENSION);

                Files.copy(resourceDirectoryCsv, testFileCsv, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.error("Copy files to accepted dir failed");
        }
    }
}
