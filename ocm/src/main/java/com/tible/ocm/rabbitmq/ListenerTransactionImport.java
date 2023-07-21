package com.tible.ocm.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Imports files to MongoDB.
 */
@Slf4j
@Component
public class ListenerTransactionImport {

    private final TransactionImportService transactionImportService;

    public ListenerTransactionImport(TransactionImportService transactionImportService) {
        this.transactionImportService = transactionImportService;
    }

    @RabbitListener(bindings = {
        @QueueBinding(value = @Queue(value = "TransactionImport"), key = "transactionImport", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC, durable = "true"))})
    public void receiveMessage(@Payload final TransactionFilePayload payload) {
        log.debug("Handle via normal listener {}", payload.getName());
        transactionImportService.handleTransactionFilePayload(payload, false);
    }
}
