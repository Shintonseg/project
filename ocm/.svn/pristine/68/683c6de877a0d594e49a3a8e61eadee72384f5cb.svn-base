package com.tible.ocm.rabbitmq;

import com.tible.hawk.core.exceptions.CoreOperationNotSupportedException;
import com.tible.hawk.core.rabbitmq.model.ResponseMessage;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@NoArgsConstructor
public class PublisherTransactionExport {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublisherTransactionExport.class);

    private TopicExchange topicExchange;
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Value("${routing-key.transaction-export}")
    private String transactionExportRoutingKey;

    public void publishToQueue(String transactionId) {
        try {
            if (!asyncRabbitTemplate.isRunning()) {
                LOGGER.info("Starting rabbit template.");
                asyncRabbitTemplate.start();
            }

            final AsyncRabbitTemplate.RabbitConverterFuture<ResponseMessage> response =
                    asyncRabbitTemplate.convertSendAndReceive(topicExchange.getName(), transactionExportRoutingKey, transactionId);

            response.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(ResponseMessage responseMessage) {
                    LOGGER.info("Response for transaction export id {} is: {}", transactionId, responseMessage.getMessage());
                }

                @Override
                public void onFailure(Throwable ex) {
                    if (ex instanceof CoreOperationNotSupportedException) {
                        LOGGER.info("Try to requeue transaction export id {}", transactionId);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    @Autowired
    public void setTopicExchange(TopicExchange topicExchange) {
        this.topicExchange = topicExchange;
    }

    @Autowired
    public void setAsyncRabbitTemplate(@Qualifier("asyncRabbitTemplateTransactionExport") AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
    }
}
