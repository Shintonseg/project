package com.tible.ocm.rabbitmq;

import com.tible.hawk.core.exceptions.CoreOperationNotSupportedException;
import com.tible.hawk.core.rabbitmq.model.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Component
public class PublisherTransactionImportBigFiles {

    private TopicExchange topicExchange;
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Value("${routing-key.transaction-import-big-files}")
    private String transactionImportBigFilesRoutingKey;

    @Autowired
    public PublisherTransactionImportBigFiles() {
    }

    public void publishToQueue(TransactionFilePayload transactionFilePayload) {
        try {
            if (!asyncRabbitTemplate.isRunning()) {
                log.info("Starting rabbit template.");
                asyncRabbitTemplate.start();
            }

            final AsyncRabbitTemplate.RabbitConverterFuture<ResponseMessage> response =
                    asyncRabbitTemplate.convertSendAndReceive(topicExchange.getName(), transactionImportBigFilesRoutingKey, transactionFilePayload);

            response.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(ResponseMessage responseMessage) {
                    log.info("Response for transaction file name (Big files) {} is: {}", transactionFilePayload.getName(), responseMessage.getMessage());
                }

                @Override
                public void onFailure(Throwable ex) {
                    if (ex instanceof CoreOperationNotSupportedException) {
                        log.info("Try to requeue transaction file name (Big files) {}", transactionFilePayload.getName());
                    }
                }
            });
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Autowired
    public void setTopicExchange(TopicExchange topicExchange) {
        this.topicExchange = topicExchange;
    }

    @Autowired
    public void setAsyncRabbitTemplate(@Qualifier("asyncRabbitTemplateTransactionImportBigFiles") AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
    }
}
