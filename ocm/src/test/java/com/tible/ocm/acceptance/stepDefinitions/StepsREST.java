package com.tible.ocm.acceptance.stepDefinitions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.models.BaseTask;
import com.tible.hawk.core.models.BaseTaskParameter;
import com.tible.hawk.core.services.BaseTaskService;
import com.tible.ocm.acceptance.dto.OauthToken;
import com.tible.ocm.acceptance.utils.TestCase;
import com.tible.ocm.acceptance.utils.TransactionFileParser;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.OcmStatus;
import com.tible.ocm.models.OcmTaskType;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.repositories.mongo.TransactionArticleRepository;
import com.tible.ocm.repositories.mongo.TransactionRepository;
import com.tible.ocm.repositories.mysql.ExistingTransactionRepository;
import com.tible.ocm.repositories.mysql.ExportedTransactionRepository;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.tible.ocm.acceptance.utils.TestCase.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class StepsREST extends AbstractSteps {

    private final TransactionRepository transactionRepository;
    private final ExistingTransactionRepository existingTransactionRepository;
    private final BaseTaskService<BaseTask, BaseTaskParameter> taskService;
    private final ObjectMapper objectMapper;
    private final ExportedTransactionRepository exportedTransactionRepository;
    private final TransactionArticleRepository transactionArticleRepository;

    private TransactionDto transactionDto;
    private OauthToken token;
    private OcmTransactionResponse response;
    private String transactionNumber = "111111100000000000116";
    private String testNumber;
    private Integer caseNumber;

    public StepsREST(TransactionRepository transactionRepository,
                     ExistingTransactionRepository existingTransactionRepository,
                     BaseTaskService<BaseTask, BaseTaskParameter> taskService,
                     ObjectMapper objectMapper,
                     TransactionFileParser transactionFileParser,
                     @Value("${tible-user.username}") String username,
                     @Value("${tible-user.password}") String password,
                     ExportedTransactionRepository exportedTransactionRepository,
                     TransactionArticleRepository transactionArticleRepository) {
        super(transactionFileParser, username, password);
        this.transactionRepository = transactionRepository;
        this.existingTransactionRepository = existingTransactionRepository;
        this.taskService = taskService;
        this.objectMapper = objectMapper;
        this.exportedTransactionRepository = exportedTransactionRepository;
        this.transactionArticleRepository = transactionArticleRepository;
    }

    @After
    public void cleanUp() {
        if (transactionDto != null) {
            cleanUpFilesAndDb(transactionDto.getTransactionNumber());
        }
    }

    @Given("^(.+) json file with (.+) and transaction exists in (.+) directory$")
    public void json_file_with_transaction_exists(String fileName, String version, String testNumber) throws IOException {
        this.testNumber = testNumber;

        Path resourceDirectory = Paths.get("src", "test", "resources", "testFiles", "rest", testNumber, version, fileName + JSON_EXTENSION);
        String jsonString = FileUtils.readFileToString(resourceDirectory.toFile(), StandardCharsets.UTF_8);

        transactionDto = objectMapper.readValue(jsonString.getBytes(), TransactionDto.class);

        transactionFileParser.processTransactionDto(IP_ADDRESS, COMPANY_NUMBER, testNumber, null, transactionDto);
        token = getToken();
    }

    @Given("^(.+) json file with (.+) and transaction exists in (.+) directory for case (.+)$")
    public void json_file_with_transaction_exists_for_case(String fileName, String version, String testNumber, Integer caseNumber) throws IOException {
        this.testNumber = testNumber;
        this.caseNumber = caseNumber;

        Path resourceDirectory = Paths.get("src", "test", "resources", "testFiles", "rest", testNumber, version, fileName + JSON_EXTENSION);
        String jsonString = FileUtils.readFileToString(resourceDirectory.toFile(), StandardCharsets.UTF_8);

        transactionDto = objectMapper.readValue(jsonString.getBytes(), TransactionDto.class);
        this.transactionNumber = transactionDto.getTransactionNumber();

        transactionFileParser.processTransactionDto(IP_ADDRESS, COMPANY_NUMBER, testNumber, caseNumber, transactionDto);
        token = getToken();
    }

    @When("send transaction create request")
    public void send_transaction_create_request() {
        response = WEB_CLIENT
                .post()
                .uri("/transaction/create")
                .headers(httpHeaders -> httpHeaders.add("X-Client-IP", IP_ADDRESS))
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token.getAccessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(transactionDto))
                .retrieve()
                .bodyToMono(OcmTransactionResponse.class)
                .block();
    }

    @Then("transaction is rejected with error")
    public void transaction_is_rejected_with_error() throws IOException {
        Path rejectedFilePathError = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(transactionNumber + ERROR_EXTENSION);
        List<String> expectedErrorMessages = getExpectedErrorMessageByTestNumber(testNumber);

        await().atMost(TIMEOUT, SECONDS).until(() -> Files.exists(rejectedFilePathError));

        List<String> errorMessages = getErrorMessagesFromFile(rejectedFilePathError);

        assertTrue(Files.exists(rejectedFilePathError));
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

    @Then("rest transaction saved and accepted")
    public void transaction_saved_and_accepted() {
        if (!SCENARIO_112_10.getNumber().equals(testNumber) && !SCENARIO_112_200.getNumber().equals(testNumber)) {
            await().atMost(TIMEOUT, SECONDS).until(() -> !transactionRepository.findAll().isEmpty());
        } else {
            await().atMost(7000, SECONDS).until(() -> !transactionRepository.findAll().isEmpty());
            await().atMost(7000, SECONDS).until(() -> !Files.exists(
                    PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("inQueueRest").resolve(IP_ADDRESS)
                            .resolve(transactionNumber + JSON_EXTENSION)));
        }

        Transaction actualTransaction = transactionRepository.findAll().get(0);
        assertNotNull(actualTransaction);
        assertEquals(1, transactionRepository.findAll().size());
        assertEquals(transactionDto.getTransactionNumber(), transactionRepository.findAll().get(0).getTransactionNumber());
        assertEquals(transactionDto.getTransactionNumber(), response.getTransactionNumber());
    }

    @Then("^errorFile is created and moved to the (.*)$")
    public void file_is_moved_to_alreadyExists_directory(String directory) {
        Path filePath;
        switch (directory) {
            case "alreadyExists":
                filePath = PATH_TO_ALREADY_EXISTS_TRANSACTION_DIRECTORY.resolve(transactionNumber + ERROR_EXTENSION);
                break;
            case "rejected":
                filePath = PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(transactionNumber + ERROR_EXTENSION);
                break;
            case "failed":
                filePath = PATH_TO_FAILED_TRANS_DIRECTORY.resolve(IP_ADDRESS).resolve(transactionNumber + ERROR_EXTENSION);
                break;
            default:
                filePath = PATH_TO_ACCEPTED_TRANSACTIONS_DIRECTORY.resolve(transactionNumber + "-" + COMPANY_NUMBER + CSV_EXTENSION);
                break;
        }

        if (SCENARIO_132.getNumber().equals(testNumber)) {
            await().atMost(TIMEOUT, SECONDS).until(() -> !transactionRepository.findAll().isEmpty());
            await().atMost(TIMEOUT, SECONDS).until(() -> transactionArticleRepository.count() == transactionRepository.findAll().get(0).getTotal());
            taskService.manualRun(OcmTaskType.TransactionToFileExporter);
            await().atMost(TIMEOUT, SECONDS).until(() -> !exportedTransactionRepository.findAll().isEmpty());
        }

        await().atMost(TIMEOUT, SECONDS).until(() -> Files.exists(filePath));

        if (!SCENARIO_129.getNumber().equals(testNumber) &&
                !SCENARIO_128.getNumber().equals(testNumber) &&
                !SCENARIO_131.getNumber().equals(testNumber) &&
                !SCENARIO_132.getNumber().equals(testNumber)) {
            if (caseNumber == 1) {
                assertEquals(1, transactionRepository.findAll().size());
            } else if (caseNumber == 2) {
                assertEquals(1, existingTransactionRepository.findAll().size());
            }
        } else {
            assertTrue(existingTransactionRepository.findAll().isEmpty());
        }

        assertTrue(Files.exists(filePath));
    }

    @Then("^response is (.+)$")
    public void response_is(String status) {
        assertEquals(status.toLowerCase(), response.getStatus().name().toLowerCase());

        if (OcmStatus.DECLINED.title.equalsIgnoreCase(status)) {
            assertEquals(0, transactionRepository.findAll().size());
        }
    }

    private List<String> getExpectedErrorMessageByTestNumber(String testNumber) {
        String expectedError = "SCENARIO_" + testNumber;
        return TestCase.valueOf(expectedError).getRestErrorMessages();
    }
}
