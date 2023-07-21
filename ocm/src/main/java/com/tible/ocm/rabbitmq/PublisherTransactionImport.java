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
public class PublisherTransactionImport {

    private TopicExchange topicExchange;
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Value("${routing-key.transaction-import}")
    private String transactionImportRoutingKey;

    @Autowired
    public PublisherTransactionImport() {
    }

    public void publishToQueue(TransactionFilePayload transactionFilePayload) {
        try {
            if (!asyncRabbitTemplate.isRunning()) {
                log.info("Starting rabbit template.");
                asyncRabbitTemplate.start();
            }

            final AsyncRabbitTemplate.RabbitConverterFuture<ResponseMessage> response =
                    asyncRabbitTemplate.convertSendAndReceive(topicExchange.getName(), transactionImportRoutingKey, transactionFilePayload);

            response.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(ResponseMessage responseMessage) {
                    log.info("Response for transaction file name {} is: {}", transactionFilePayload.getName(), responseMessage.getMessage());
                }

                @Override
                public void onFailure(Throwable ex) {
                    if (ex instanceof CoreOperationNotSupportedException) {
                        log.info("Try to requeue transaction file name {}", transactionFilePayload.getName());
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
    public void setAsyncRabbitTemplate(@Qualifier("asyncRabbitTemplateTransactionImport") AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
    }
}
