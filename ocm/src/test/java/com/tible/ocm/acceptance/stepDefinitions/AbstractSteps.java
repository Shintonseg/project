package com.tible.ocm.acceptance.stepDefinitions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tible.ocm.acceptance.dto.ErrorFileDto;
import com.tible.ocm.acceptance.dto.OauthToken;
import com.tible.ocm.acceptance.utils.TransactionFileParser;
import com.tible.ocm.utils.OcmFileUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


@Component
public abstract class AbstractSteps {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSteps.class);
    private static final String ROOT_DIR = "testSync";
//    private static final Path PATH_TO_REJECTED_TRANSACTIONS_DIRECTORY = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("rejected");
//    private static final Path PATH_TO_ACCEPTED_TRANSACTIONS_DIRECTORY = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("accepted");

    protected static final int TIMEOUT = 75;
    protected static final String ERROR_EXTENSION = ".error";
    protected static final String JSON_EXTENSION = ".json";
    protected static final String CSV_EXTENSION = ".csv";
    protected static final String HASH_EXTENSION = ".hash";
    protected static final String COMPANY_NUMBER = "1111";
    protected static final String IP_ADDRESS = "192.168.85.7";
    protected static final Path PATH_TO_RVM_TRANSACTIONS_DIRECTORY = Path.of(ROOT_DIR + "/RVM/transactions");
    protected static final Path PATH_TO_TRANS_DIRECTORY = Path.of(ROOT_DIR + "/" + IP_ADDRESS + "/TRANS");
    protected static final Path PATH_TO_REJECTED_TRANS_DIRECTORY = PATH_TO_TRANS_DIRECTORY.resolve("rejected");
    protected static final Path PATH_TO_FAILED_TRANS_DIRECTORY = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("failed");
    protected static final Path PATH_TO_ACCEPTED_TRANSACTIONS_DIRECTORY = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("accepted");
    protected static final Path PATH_TO_ALREADY_EXISTS_TRANSACTION_DIRECTORY = PATH_TO_RVM_TRANSACTIONS_DIRECTORY.resolve("alreadyExists");
    protected static final WebClient WEB_CLIENT = WebClient.builder().baseUrl("http://localhost:8081").build();
    protected static final Path TEST_RES_FILE_PATH = Paths.get("src", "test", "resources", "testFiles");

    protected final TransactionFileParser transactionFileParser;

    private final String username;
    private final String password;

    public AbstractSteps(TransactionFileParser transactionFileParser,
                         @Value("${tible-user.username}") String username,
                         @Value("${tible-user.password}") String password) {
        this.transactionFileParser = transactionFileParser;
        this.username = username;
        this.password = password;
    }

    protected void cleanUpFilesAndDb(String fileName) {
        transactionFileParser.cleanUpDb();

        try {
//            Files.deleteIfExists(PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + ERROR_EXTENSION));
//            Files.deleteIfExists(PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + CSV_EXTENSION));
//            Files.deleteIfExists(PATH_TO_REJECTED_TRANS_DIRECTORY.resolve(fileName + HASH_EXTENSION));
//
//            Files.deleteIfExists(PATH_TO_REJECTED_TRANSACTIONS_DIRECTORY.resolve(COMPANY_NUMBER).resolve(fileName + ERROR_EXTENSION));
//            Files.deleteIfExists(PATH_TO_REJECTED_TRANSACTIONS_DIRECTORY.resolve(COMPANY_NUMBER).resolve(fileName + CSV_EXTENSION));
//            Files.deleteIfExists(PATH_TO_REJECTED_TRANSACTIONS_DIRECTORY.resolve(COMPANY_NUMBER).resolve(fileName + HASH_EXTENSION));
//            Files.deleteIfExists(PATH_TO_ALREADY_EXISTS_TRANSACTION_DIRECTORY.resolve(fileName + CSV_EXTENSION));
//            Files.deleteIfExists(PATH_TO_ALREADY_EXISTS_TRANSACTION_DIRECTORY.resolve(fileName + HASH_EXTENSION));
//            FileUtils.deleteDirectory(new File(PATH_TO_ACCEPTED_TRANSACTIONS_DIRECTORY.toUri()));
//            Files.deleteIfExists(Path.of(ROOT_DIR));
            OcmFileUtils.clearDirectory(Path.of(ROOT_DIR));
        } catch (Exception e) {
            LOG.info("Can't delete files");
        }
        LOG.info("Deletion successfully");
    }

    protected OauthToken getToken() {
        return WEB_CLIENT
                .post()
                .uri("/oauth/token")
                .headers(httpHeaders -> httpHeaders.setBasicAuth(username, password))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                .retrieve()
                .bodyToMono(OauthToken.class)
                .block();
    }

    protected List<String> getErrorMessagesFromFile(Path rejectedFilePathError) throws IOException {
        String errorFileContent = FileUtils.readFileToString(rejectedFilePathError.toFile(), StandardCharsets.UTF_8);

        Gson gson = new GsonBuilder().create();
        ErrorFileDto errorFile = gson.fromJson(errorFileContent, ErrorFileDto.class);

        return errorFile.getImportMessages().stream().map(ErrorFileDto.MessageDetails::getMessage).collect(Collectors.toList());
    }
}
