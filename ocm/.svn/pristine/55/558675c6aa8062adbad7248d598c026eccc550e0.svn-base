package com.tible.ocm.acceptance.stepDefinitions;

import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.acceptance.utils.TestCase;
import com.tible.ocm.acceptance.utils.TransactionFileParser;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.repositories.mongo.TransactionArticleRepository;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.repositories.mysql.ExistingTransactionRepository;
import com.tible.ocm.repositories.mysql.ExportedTransactionRepository;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static com.tible.ocm.acceptance.utils.TestCase.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class StepsSFTP extends AbstractSteps {

    private final TransactionRepository transactionRepository;
    private final ExistingTransactionRepository existingTransactionRepository;
    private final BaseTaskService<BaseTask, BaseTaskParameter> taskService;
    private final ExportedTransactionRepository exportedTransactionRepository;
    private final TransactionArticleRepository transactionArticleRepository;

    private String fileName;
    private String testNumber;
    private Integer caseNumber;

    public StepsSFTP(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                     TransactionFileParser transactionFileParser,
                     TransactionRepository transactionRepository,
                     ExistingTransactionRepository existingTransactionRepository,
                     @Value("${tible-user.username}") String username,
                     @Value("${tible-user.password}") String password,
                     ExportedTransactionRepository exportedTransactionRepository,
                     TransactionArticleRepository transactionArticleRepository) {
        super(transactionFileParser, username, password);
        this.taskService = taskService;
        this.transactionRepository = transactionRepository;
        this.existingTransactionRepository = existingTransactionRepository;
        this.exportedTransactionRepository = exportedTransactionRepository;
        this.transactionArticleRepository = transactionArticleRepository;
    }

    @After
    public void cleanUp() {
        if (fileName != null) {
            cleanUpFilesAndDb(fileName);
        }
    }

    @Given("^company delivers the transaction file with (.+) and name (.+) from (.+) directory$")
    public void company_delivers_the_transaction_file(String version, String fileName, String testNumber) throws IOException {
        this.fileName = fileName;
        this.testNumber = testNumber;

        Path testFileCsv = moveFilesFromResourceDirectoryToTRANS(version);

        transactionFileParser.parseTestTransactionFile(testFileCsv, IP_ADDRESS, COMPANY_NUMBER, testNumber, null);
    }

    @Given("^company delivers the transaction file with (.+) and name (.+) from (.+) directory for case (.+)$")
    public void company_delivers_the_transaction_file(String version, String fileName, String testNumber, Integer caseNumber) throws IOException {
        this.fileName = fileName;
        this.testNumber = testNumber;
        this.caseNumber = caseNumber;

        Path testFileCsv = moveFilesFromResourceDirectoryToTRANS(version);

        transactionFileParser.parseTestTransactionFile(testFileCsv, IP_ADDRESS, COMPANY_NUMBER, testNumber, caseNumber);
    }

    @When("file is being processed")
    public void file_is_being_processed() {
        taskService.manualRun(OcmTaskType.TransactionPerCompanyFileImporter);
    }

    @Then("^file is rejected with an error$")
    public void file_is_rejected_with_an_error() throws IOException {
        Path rejectedFilePathCsv = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + CSV_EXTENSION);
        Path rejectedFilePathError = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + ERROR_EXTENSION);
        Path rejectedFilePathHash = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + HASH_EXTENSION);

        await().atMost(TIMEOUT, SECONDS).until(() -> Files.exists(rejectedFilePathError));

        List<String> errorMessages = getErrorMessagesFromFile(rejectedFilePathError);

        List<String> expectedErrorMessages = getExpectedErrorMessageByTestNumber(testNumber);

        assertEquals(0, transactionRepository.findAll().size());
        assertTrue(Files.exists(rejectedFilePathCsv));
        assertTrue(Files.exists(rejectedFilePathError));
        if (!SCENARIO_109.getNumber().equals(testNumber)) {
            assertTrue(Files.exists(rejectedFilePathHash));
        }
        assertTrue(errorMessages.stream().anyMatch(s -> s.matches(expectedErrorMessages.get(0))));
        if (SCENARIO_111.getNumber().equals(testNumber) && caseNumber == 1) {
            assertTrue(errorMessages.get(0).contains("lower"));
        }
        if (SCENARIO_111.getNumber().equals(testNumber) && caseNumber == 2) {
            assertTrue(errorMessages.get(0).contains("higher"));
        }
        if (SCENARIO_116.getNumber().equals(testNumber)) {
            assertTrue(errorMessages.get(0).matches(expectedErrorMessages.get(0)));
            assertTrue(errorMessages.get(1).matches(expectedErrorMessages.get(1)));
        }
    }

    @Then("^file moved to (.+)$")
    public void file_moved_to_directory(String directory) {
        Path filePathCsv;
        Path filePathHash;
        switch (directory) {
            case "alreadyExists":
                filePathCsv = PATH_TO_ALREADY_EXISTS_TRANSACTION_DIRECTORY.resolve(fileName + CSV_EXTENSION);
                filePathHash = PATH_TO_ALREADY_EXISTS_TRANSACTION_DIRECTORY.resolve(fileName + HASH_EXTENSION);
                break;
            case "rejected":
                filePathCsv = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + CSV_EXTENSION);
                filePathHash = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + HASH_EXTENSION);
                break;
            case "failed":
                filePathCsv = PATH_TO_FAILED_TRANS_DIRECTORY.resolve(IP_ADDRESS).resolve(fileName + CSV_EXTENSION);
                filePathHash = PATH_TO_FAILED_TRANS_DIRECTORY.resolve(IP_ADDRESS).resolve(fileName + HASH_EXTENSION);
                break;
            default:
                filePathCsv = PATH_TO_ACCEPTED_TRANSACTIONS_DIRECTORY.resolve(fileName + "-" + COMPANY_NUMBER + CSV_EXTENSION);
                filePathHash = PATH_TO_ACCEPTED_TRANSACTIONS_DIRECTORY.resolve(fileName + "-" + COMPANY_NUMBER + HASH_EXTENSION);
                break;
        }

        if (SCENARIO_132.getNumber().equals(testNumber)) {
            await().atMost(TIMEOUT, SECONDS).until(() -> !transactionRepository.findAll().isEmpty());
            await().atMost(TIMEOUT, SECONDS).until(() -> transactionArticleRepository.count() == transactionRepository.findAll().get(0).getTotal());
            taskService.manualRun(OcmTaskType.TransactionToFileExporter);
            await().atMost(TIMEOUT, SECONDS).until(() -> !exportedTransactionRepository.findAll().isEmpty());
        }

        await().atMost(TIMEOUT, SECONDS).until(() -> Files.exists(filePathHash));

        if (SCENARIO_103.getNumber().equals(testNumber)) {
            if (caseNumber == 1) {
                assertEquals(1, transactionRepository.findAll().size());
            } else if (caseNumber == 2) {
                assertEquals(1, existingTransactionRepository.findAll().size());
            }
        }

        assertTrue(Files.exists(filePathCsv));
        assertTrue(Files.exists(filePathHash));
    }

    @Then("transaction saved and accepted")
    public void transaction_saved_and_accepted() {
        Path fileCsv = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("backup").resolve(IP_ADDRESS).resolve(fileName + CSV_EXTENSION);
        Path fileHash = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("backup").resolve(IP_ADDRESS).resolve(fileName + HASH_EXTENSION);
        if (!SCENARIO_112_10.getNumber().equals(testNumber) && !SCENARIO_112_200.getNumber().equals(testNumber)) {
            await().atMost(TIMEOUT, SECONDS).until(() -> !transactionRepository.findAll().isEmpty());
            await().atMost(TIMEOUT, SECONDS).until(() -> Files.exists(fileCsv));
            await().atMost(TIMEOUT, SECONDS).until(() -> Files.exists(fileHash));
        } else {
            await().atMost(7000, SECONDS).until(() -> !transactionRepository.findAll().isEmpty());
            await().atMost(7000, SECONDS).until(() -> Files.exists(fileCsv));
            await().atMost(7000, SECONDS).until(() -> Files.exists(fileHash));
        }

        Transaction actualTransaction = transactionRepository.findAll().get(0);
        assertNotNull(actualTransaction);
        assertEquals(1, transactionRepository.findAll().size());
    }

    private List<String> getExpectedErrorMessageByTestNumber(String testNumber) {
        String expectedError = "SCENARIO_" + testNumber;
        return TestCase.valueOf(expectedError).getSftpErrorMessages();
    }

    private Path moveFilesFromResourceDirectoryToTRANS(String version) throws IOException {
        Files.createDirectories(PATH_TO_TRANS_DIRECTORY);

        Path SFTP_RES = TEST_RES_FILE_PATH.resolve("sftp").resolve(testNumber).resolve(version);

        Path testFileCsv = SFTP_RES.resolve(fileName + CSV_EXTENSION);
        Path testFileHash = SFTP_RES.resolve(fileName + HASH_EXTENSION);

        Path resourceDirectoryCsv = PATH_TO_TRANS_DIRECTORY.resolve(testFileCsv.getFileName());
        Path resourceDirectoryHash = PATH_TO_TRANS_DIRECTORY.resolve(testFileHash.getFileName());

        Files.copy(testFileCsv, resourceDirectoryCsv, StandardCopyOption.REPLACE_EXISTING);
        if (!SCENARIO_109.getNumber().equals(testNumber)) {
            Files.copy(testFileHash, resourceDirectoryHash, StandardCopyOption.REPLACE_EXISTING);
        }

        return resourceDirectoryCsv;
    }
}
