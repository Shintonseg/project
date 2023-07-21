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
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Imports files to MongoDB.
 */
@Slf4j
@Component
public class ListenerTransactionImportBigFiles {

    private final TransactionImportService transactionImportService;

    public ListenerTransactionImportBigFiles(TransactionImportService transactionImportService) {
        this.transactionImportService = transactionImportService;
    }

    @RabbitListener(bindings = {
        @QueueBinding(value = @Queue(value = "TransactionImportBigFiles"), key = "transactionImportBigFiles", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC, durable = "true"))})
    public void receiveMessage(@Payload final TransactionFilePayload payload) {
        log.debug("Handle via big file listener {}", payload.getName());
        transactionImportService.handleTransactionFilePayload(payload, true);
    }
}
