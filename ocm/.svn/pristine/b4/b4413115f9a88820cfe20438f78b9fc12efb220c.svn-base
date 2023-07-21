package com.tible.ocm.rabbitmq;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan(basePackages = {"com.tible.ocm.rabbitmq"})
public class OcmRabbitConfiguration {

    @Value("${exchange.topic:#{null}}")
    private String topicExchange;
    @Value("${queue.dead-letter:#{null}}")
    private String deadLetterQueue;

    @Value("${routing-key.transaction-export-response:#{null}}")
    private String transactionExportResponse;
    @Value("${queue.transaction-export-reply:#{null}}")
    private String transactionExportReplyQueue;

    @Value("${routing-key.transaction-import-response:#{null}}")
    private String transactionImportResponse;
    @Value("${queue.transaction-import-reply:#{null}}")
    private String transactionImportReplyQueue;

    @Value("${routing-key.transaction-import-big-files-response:#{null}}")
    private String transactionImportBigFilesResponse;
    @Value("${queue.transaction-import-big-files-reply:#{null}}")
    private String transactionImportBigFilesReplyQueue;

    @Value("${routing-key.transaction-import-rest-response:#{null}}")
    private String transactionImportRestResponse;
    @Value("${queue.transaction-import-rest-reply:#{null}}")
    private String transactionImportRestReplyQueue;

    @Value("${routing-key.transaction-company-confirmed-response:#{null}}")
    private String transactionCompanyConfirmedResponse;
    @Value("${queue.transaction-company-confirmed-reply:#{null}}")
    private String transactionCompanyConfirmedReplyQueue;

    private RabbitTemplate rabbitTemplate;

    @Bean
    @Qualifier("asyncRabbitTemplateTransactionExport")
    public AsyncRabbitTemplate asyncRabbitTemplateTransactionExport(ConnectionFactory connectionFactory,
                                                                    AmqpAdmin rabbitAdmin) {

        final AsyncRabbitTemplate template = new AsyncRabbitTemplate(rabbitTemplate,
                replyMessageListenerContainerTransactionExport(connectionFactory, rabbitAdmin),
                topicExchange + "/" + transactionExportResponse);

        //Timeout for 3 minute
        template.setReceiveTimeout(3 * 60 * 1000L);

        return template;
    }

    @Bean
    public SimpleMessageListenerContainer replyMessageListenerContainerTransactionExport(ConnectionFactory connectionFactory,
                                                                                         AmqpAdmin rabbitAdmin) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        messageListenerContainer.setQueues(transactionExportReplyQueue());
        messageListenerContainer.setReceiveTimeout(60000);
        messageListenerContainer.setTaskExecutor(Executors.newCachedThreadPool());
        messageListenerContainer.setAmqpAdmin(rabbitAdmin);
        messageListenerContainer.setAutoStartup(false);
        return messageListenerContainer;
    }

    @Bean
    public Queue transactionExportReplyQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueue);
        return new Queue(transactionExportReplyQueue, true, false, false, args);
    }

    @Bean
    @Qualifier("asyncRabbitTemplateTransactionImport")
    public AsyncRabbitTemplate asyncRabbitTemplateTransactionImport(ConnectionFactory connectionFactory,
                                                                    AmqpAdmin rabbitAdmin) {

        final AsyncRabbitTemplate template = new AsyncRabbitTemplate(rabbitTemplate,
                replyMessageListenerContainerTransactionImport(connectionFactory, rabbitAdmin),
                topicExchange + "/" + transactionImportResponse);

        //Timeout for 3 minute
        template.setReceiveTimeout(3 * 60 * 1000L);

        return template;
    }

    @Bean
    public SimpleMessageListenerContainer replyMessageListenerContainerTransactionImport(ConnectionFactory connectionFactory,
                                                                                         AmqpAdmin rabbitAdmin) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        messageListenerContainer.setQueues(transactionImportReplyQueue());
        messageListenerContainer.setReceiveTimeout(60000);
        messageListenerContainer.setTaskExecutor(Executors.newCachedThreadPool());
        messageListenerContainer.setAmqpAdmin(rabbitAdmin);
        messageListenerContainer.setAutoStartup(false);
        return messageListenerContainer;
    }

    @Bean
    public Queue transactionImportReplyQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueue);
        return new Queue(transactionImportReplyQueue, true, false, false, args);
    }

    @Bean
    @Qualifier("asyncRabbitTemplateTransactionImportBigFiles")
    public AsyncRabbitTemplate asyncRabbitTemplateTransactionImportBigFiles(ConnectionFactory connectionFactory,
                                                                            AmqpAdmin rabbitAdmin) {

        final AsyncRabbitTemplate template = new AsyncRabbitTemplate(rabbitTemplate,
                replyMessageListenerContainerTransactionImportBigFiles(connectionFactory, rabbitAdmin),
                topicExchange + "/" + transactionImportBigFilesResponse);

        //Timeout for 3 minute
        template.setReceiveTimeout(3 * 60 * 1000L);

        return template;
    }

    @Bean
    public SimpleMessageListenerContainer replyMessageListenerContainerTransactionImportBigFiles(ConnectionFactory connectionFactory,
                                                                                                 AmqpAdmin rabbitAdmin) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        messageListenerContainer.setQueues(transactionImportBigFilesReplyQueue());
        messageListenerContainer.setReceiveTimeout(60000);
        messageListenerContainer.setTaskExecutor(Executors.newCachedThreadPool());
        messageListenerContainer.setAmqpAdmin(rabbitAdmin);
        messageListenerContainer.setAutoStartup(false);
        return messageListenerContainer;
    }

    @Bean
    public Queue transactionImportBigFilesReplyQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueue);
        return new Queue(transactionImportBigFilesReplyQueue, true, false, false, args);
    }

    @Bean
    @Qualifier("asyncRabbitTemplateTransactionImportRest")
    public AsyncRabbitTemplate asyncRabbitTemplateTransactionImportRest(ConnectionFactory connectionFactory,
                                                                    AmqpAdmin rabbitAdmin) {

        final AsyncRabbitTemplate template = new AsyncRabbitTemplate(rabbitTemplate,
                replyMessageListenerContainerTransactionImportRest(connectionFactory, rabbitAdmin),
                topicExchange + "/" + transactionImportRestResponse);

        //Timeout for 3 minute
        template.setReceiveTimeout(3 * 60 * 1000L);

        return template;
    }

    @Bean
    public SimpleMessageListenerContainer replyMessageListenerContainerTransactionImportRest(ConnectionFactory connectionFactory,
                                                                                         AmqpAdmin rabbitAdmin) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        messageListenerContainer.setQueues(transactionImportRestReplyQueue());
        messageListenerContainer.setReceiveTimeout(10 * 1000);
        messageListenerContainer.setTaskExecutor(Executors.newCachedThreadPool());
        messageListenerContainer.setAmqpAdmin(rabbitAdmin);
        messageListenerContainer.setAutoStartup(false);
        return messageListenerContainer;
    }

    @Bean
    public Queue transactionImportRestReplyQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueue);
        return new Queue(transactionImportRestReplyQueue, true, false, false, args);
    }

    @Bean
    @Qualifier("asyncRabbitTemplateTransactionCompanyConfirmed")
    public AsyncRabbitTemplate asyncRabbitTemplateTransactionCompanyConfirmed(ConnectionFactory connectionFactory,
                                                                              AmqpAdmin rabbitAdmin) {

        final AsyncRabbitTemplate template = new AsyncRabbitTemplate(rabbitTemplate,
                replyMessageListenerContainerTransactionCompanyConfirmed(connectionFactory, rabbitAdmin),
                topicExchange + "/" + transactionCompanyConfirmedResponse);

        //Timeout for 3 minute
        template.setReceiveTimeout(3 * 60 * 1000L);

        return template;
    }

    @Bean
    public SimpleMessageListenerContainer replyMessageListenerContainerTransactionCompanyConfirmed(ConnectionFactory connectionFactory,
                                                                                                   AmqpAdmin rabbitAdmin) {
        SimpleMessageListenerContainer messageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        messageListenerContainer.setQueues(transactionCompanyConfirmedReplyQueue());
        messageListenerContainer.setReceiveTimeout(10 * 1000);
        messageListenerContainer.setTaskExecutor(Executors.newCachedThreadPool());
        messageListenerContainer.setAmqpAdmin(rabbitAdmin);
        messageListenerContainer.setAutoStartup(false);
        return messageListenerContainer;
    }

    @Bean
    public Queue transactionCompanyConfirmedReplyQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", deadLetterQueue);
        return new Queue(transactionCompanyConfirmedReplyQueue, true, false, false, args);
    }
    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
}
