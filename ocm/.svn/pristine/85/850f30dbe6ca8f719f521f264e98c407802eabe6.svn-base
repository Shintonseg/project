package com.tible.ocm.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.TransactionService;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Imports transaction received by REST to MongoDB.
 */
@Slf4j
@Component
public class ListenerTransactionImportRest {

    private final TransactionService transactionService;
    private final CompanyService companyService;
    private final DirectoryService directoryService;
    private final ObjectMapper objectMapper;

    public ListenerTransactionImportRest(TransactionService transactionService,
                                         CompanyService companyService,
                                         DirectoryService directoryService) {
        this.transactionService = transactionService;
        this.companyService = companyService;
        this.directoryService = directoryService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionImportRest"), key = "transactionImportRest", exchange =
            @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC, durable = "true"))})
    public void receiveMessage(@Payload TransactionFilePayloadRest payload) {
        log.debug("Handle via normal listener {}", payload.getName());
        final Path transactionsInQueueRestDir = directoryService.getTransactionsInQueueRestPath();
        Company company = companyService.findById(payload.getCompanyId()).orElseThrow();
        Path inQueueCompany = transactionsInQueueRestDir.resolve(company.getIpAddress());
        OcmFileUtils.checkOrCreateDirWithFullPermissions(inQueueCompany);
        Path inQueueRestFilePath = inQueueCompany.resolve(payload.getName());

        try {
            if (Files.exists(inQueueRestFilePath)) {
                TransactionDto transactionDto;
                try {
                    transactionDto = objectMapper.readValue(
                            FileUtils.readFileToString(inQueueRestFilePath.toFile(), StandardCharsets.UTF_8),
                            TransactionDto.class);
                } catch (IOException e) {
                    log.warn("Reading {} failed", inQueueRestFilePath, e);
                    throw new RuntimeException("Reading transaction rest file failed", e);
                }

                transactionService.saveTransaction(transactionDto, company);
                deleteFile(inQueueRestFilePath);
                log.info("Processed transaction file from queue {}", payload.getName());
            } else {
                log.info("Transaction file {} is missed", inQueueRestFilePath);
            }
        } catch (Exception e) {
            final Path transactionsFailedRestDir = directoryService.getTransactionsFailedRestPath();
            Path companyFailedPath = transactionsFailedRestDir.resolve(company.getIpAddress());
            OcmFileUtils.checkOrCreateDirWithFullPermissions(companyFailedPath);
            ImportHelper.moveIfExists(companyFailedPath, inQueueRestFilePath);
            log.warn("Failed to process transaction rest file {}. Move file to failed directory", inQueueRestFilePath);
        }
    }

    private void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete transaction rest file: {}", file, e);
        }
    }
}