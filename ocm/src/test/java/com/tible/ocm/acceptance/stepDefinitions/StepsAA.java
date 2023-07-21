package com.tible.ocm.acceptance.stepDefinitions;

import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.acceptance.utils.TestCase;
import com.tible.ocm.acceptance.utils.TransactionFileParser;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.repositories.mysql.ExistingTransactionRepository;
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

import static com.tible.ocm.acceptance.utils.TestCase.SCENARIO_109;
import static com.tible.ocm.utils.ImportHelper.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class StepsAA extends AbstractSteps {

    private static final Path AA_TEST_RES = TEST_RES_FILE_PATH.resolve("AA");
    private final TransactionRepository transactionRepository;
    private final ExistingTransactionRepository existingTransactionRepository;
    private final BaseTaskService<BaseTask, BaseTaskParameter> taskService;

    private String fileName;
    private String testNumber;
    private Integer caseNumber;

    public StepsAA(BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                   TransactionFileParser transactionFileParser,
                   TransactionRepository transactionRepository,
                   ExistingTransactionRepository existingTransactionRepository,
                   @Value("${tible-user.username}") String username,
                   @Value("${tible-user.password}") String password) {
        super(transactionFileParser, username, password);
        this.taskService = taskService;
        this.transactionRepository = transactionRepository;
        this.existingTransactionRepository = existingTransactionRepository;
    }

    @After
    public void cleanUp() {
        if (fileName != null) {
            cleanUpFilesAndDb(fileName);
        }
    }

    @Given("^company delivers the AA transaction file with (.+) and name (.+) from (.+) directory for case (.+)$")
    public void company_delivers_the_transaction_file(String version, String fileName, String testNumber, Integer caseNumber) throws IOException {
        this.fileName = fileName;
        this.testNumber = testNumber;
        this.caseNumber = caseNumber;

        Path testFileCsv = moveFilesFromResourceDirectoryToTRANS(version);

        transactionFileParser.parseTestAATransactionFile(testFileCsv, IP_ADDRESS, COMPANY_NUMBER, testNumber, caseNumber);
    }

    @When("AA files is being processed")
    public void file_is_being_processed() {
        taskService.manualRun(OcmTaskType.AAFilesPerCompanyImporter);
    }

    @Then("^AA files with is rejected with an error$")
    public void file_is_rejected_with_an_error() throws IOException {
        Path errorFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + ERROR_EXTENSION);
        Path readyFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + READY_FILE_FORMAT);
        Path readyHashFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + READY_HASH_FILE_FORMAT);
        Path batchFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + BATCH_FILE_FORMAT);
        Path batchHashFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + BATCH_HASH_FILE_FORMAT);
        Path slsFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + SLS_FILE_FORMAT);
        Path slsHashFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + SLS_HASH_FILE_FORMAT);
        Path nlsFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + NLS_FILE_FORMAT);
        Path nlsHashFilePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + NLS_HASH_FILE_FORMAT);

        await().atMost(10000, SECONDS).until(() -> Files.exists(errorFilePath));

        List<String> errorMessages = getErrorMessagesFromFile(errorFilePath);
        String expectedErrorMessage = getExpectedErrorMessageByTestNumber(testNumber);

        assertEquals(0, transactionRepository.findAll().size());
        assertTrue(Files.exists(errorFilePath));

        assertTrue(Files.exists(readyFilePath));
        assertTrue(Files.exists(slsFilePath));
        assertTrue(Files.exists(batchFilePath));
        assertTrue(Files.exists(nlsFilePath));

        if (!SCENARIO_109.getNumber().equals(testNumber)) {
            assertTrue(Files.exists(slsHashFilePath));
            assertTrue(Files.exists(batchHashFilePath));
            assertTrue(Files.exists(readyHashFilePath));
            assertTrue(Files.exists(nlsHashFilePath));
        }
        assertTrue(errorMessages.stream().anyMatch(s -> s.matches(expectedErrorMessage)));
    }

    private Path moveFilesFromResourceDirectoryToTRANS(String version) throws IOException {
        Files.createDirectories(PATH_TO_TRANS_DIRECTORY);

        Path testFilesPath = AA_TEST_RES.resolve(testNumber).resolve(version);

        Path readyFilePath = testFilesPath.resolve(fileName + READY_FILE_FORMAT);
        Path readyHashFilePath = testFilesPath.resolve(fileName + READY_HASH_FILE_FORMAT);
        Path batchFilePath = testFilesPath.resolve(fileName + BATCH_FILE_FORMAT);
        Path batchHashFilePath = testFilesPath.resolve(fileName + BATCH_HASH_FILE_FORMAT);
        Path slsFilePath = testFilesPath.resolve(fileName + SLS_FILE_FORMAT);
        Path slsHashFilePath = testFilesPath.resolve(fileName + SLS_HASH_FILE_FORMAT);
        Path nlsFilePath = testFilesPath.resolve(fileName + NLS_FILE_FORMAT);
        Path nlsHashFilePath = testFilesPath.resolve(fileName + NLS_HASH_FILE_FORMAT);

        Path readyPath = PATH_TO_TRANS_DIRECTORY.resolve(readyFilePath.getFileName());
        Path readyHashPath = PATH_TO_TRANS_DIRECTORY.resolve(readyHashFilePath.getFileName());
        Path batchPath = PATH_TO_TRANS_DIRECTORY.resolve(batchFilePath.getFileName());
        Path batchHashPath = PATH_TO_TRANS_DIRECTORY.resolve(batchHashFilePath.getFileName());
        Path slsPath = PATH_TO_TRANS_DIRECTORY.resolve(slsFilePath.getFileName());
        Path slsHashPath = PATH_TO_TRANS_DIRECTORY.resolve(slsHashFilePath.getFileName());
        Path nlsPath = PATH_TO_TRANS_DIRECTORY.resolve(nlsFilePath.getFileName());
        Path nlsHashPath = PATH_TO_TRANS_DIRECTORY.resolve(nlsHashFilePath.getFileName());

        Files.copy(readyFilePath, readyPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(batchFilePath, batchPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(slsFilePath, slsPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(nlsFilePath, nlsPath, StandardCopyOption.REPLACE_EXISTING);

        if (!SCENARIO_109.getNumber().equals(testNumber)) {
            Files.copy(readyHashFilePath, readyHashPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(batchHashFilePath, batchHashPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(nlsHashFilePath, nlsHashPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(slsHashFilePath, slsHashPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return batchPath;
    }

    private String getExpectedErrorMessageByTestNumber(String testNumber) {
        String expectedError = "SCENARIO_" + testNumber;
        return TestCase.valueOf(expectedError).getAAErrorMessage();
    }
}
