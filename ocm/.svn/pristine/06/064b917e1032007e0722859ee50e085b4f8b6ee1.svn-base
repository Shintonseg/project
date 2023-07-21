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
public class PublisherTransactionCompanyConfirmed {

    private TopicExchange topicExchange;
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Value("${routing-key.transaction-company-confirmed}")
    private String transactionCompanyConfirmedRoutingKey;

    @Autowired
    public PublisherTransactionCompanyConfirmed() {
    }

    public void publishToQueue(TransactionCompanyConfirmedPayload transactionCompanyConfirmedPayload) {
        try {
            if (!asyncRabbitTemplate.isRunning()) {
                log.info("Starting rabbit template.");
                asyncRabbitTemplate.start();
            }

            final AsyncRabbitTemplate.RabbitConverterFuture<ResponseMessage> response =
                    asyncRabbitTemplate.convertSendAndReceive(topicExchange.getName(), transactionCompanyConfirmedRoutingKey, transactionCompanyConfirmedPayload);

            response.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(ResponseMessage responseMessage) {
                    log.info("Response for transaction company confirmed {} is: {}", transactionCompanyConfirmedPayload, responseMessage.getMessage());
                }

                @Override
                public void onFailure(Throwable ex) {
                    if (ex instanceof CoreOperationNotSupportedException) {
                        log.info("Try to requeue transaction company confirmed {}", transactionCompanyConfirmedPayload);
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
    public void setAsyncRabbitTemplate(@Qualifier("asyncRabbitTemplateTransactionCompanyConfirmed") AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
    }
}
