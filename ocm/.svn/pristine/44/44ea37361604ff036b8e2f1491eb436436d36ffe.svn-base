package com.tible.ocm.rabbitmq;

import com.tible.hawk.core.controllers.helpers.BaseMessageType;
import com.tible.hawk.core.controllers.helpers.MailData;
import com.tible.hawk.core.services.BaseMailService;
import com.tible.hawk.core.utils.FileUtils;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.models.mongo.TransactionArticle;
import com.tible.ocm.services.*;
import com.tible.ocm.utils.ImportHelper;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.tible.hawk.core.utils.ExportHelper.writeValues;
import static com.tible.ocm.models.CsvRecordType.*;
import static com.tible.ocm.utils.ImportRvmSupplierHelper.DATETIMEFORMATTER;
import static java.util.Optional.ofNullable;

/**
 * Builds [transactionNumber]-[companyNumber].csv files based on data from MongoDB.
 * Example: 00000000000000022001-010001.csv
 */
@Slf4j
@Component
public class ListenerTransactionExport {

    @Value("#{'${mail-to.file-export-failed}'.split(',')}")
    private List<String> fileExportFailedMailTo;

    @Value("${spring.application.name}")
    private String applicationName;

    private final DirectoryService directoryService;
    private final CompanyService companyService;
    private final TransactionService transactionService;
    private final ExportedTransactionService exportedTransactionService;
    private final BaseMailService mailService;
    private final ExistingBagService existingBagService;
    private final ExistingTransactionService existingTransactionService;

    ListenerTransactionExport(TransactionService transactionService,
                              DirectoryService directoryService,
                              CompanyService companyService,
                              ExportedTransactionService exportedTransactionService,
                              BaseMailService mailService,
                              ExistingBagService existingBagService,
                              ExistingTransactionService existingTransactionService) {
        this.transactionService = transactionService;
        this.directoryService = directoryService;
        this.companyService = companyService;
        this.exportedTransactionService = exportedTransactionService;
        this.mailService = mailService;
        this.existingBagService = existingBagService;
        this.existingTransactionService = existingTransactionService;
    }

    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionExport"), key = "transactionExport", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC, durable = "true"))})
    public void receiveMessage(@Payload final String transactionExport) {
        try {
            transactionService.findById(transactionExport).ifPresent(transaction -> {
                final String transactionNumber = transaction.getTransactionNumber();
                if (transaction.getType().equals(ImportHelper.FILE_TYPE_AA_TRANSACTION) || transaction.getType().equals(ImportHelper.FILE_TYPE_TRANSACTION)) {
                    Optional<Company> optionalCompany = companyService.findById(transaction.getCompanyId());
                    if (optionalCompany.isPresent() && existingTransactionService.lazyCheckIsTransactionAlreadyExists(transactionNumber, optionalCompany.get().getRvmOwnerNumber())) {
                        transactionService.delete(transaction);
                        log.info("Removed database transaction with transaction number {} because it already exists, company number is {}", transactionNumber, optionalCompany.get().getNumber());
                    }
                } else {
                    if (existingBagService.lazyCheckIsBagAlreadyExists(transactionNumber)) {
                        transactionService.delete(transaction);
                        log.info("Removed database transaction (bag) with transaction number {} because it already exists", transactionNumber);
                    }
                }

                if (transaction.getTotal() != null &&
                        !transaction.getTotal().equals(transactionService.countAllByTransactionId(transaction.getId()))) {
                    String message = String.format("Total article amount %d of transaction does not equal the count of transaction articles %d for transaction number %s",
                            transaction.getTotal(), transactionService.countAllByTransactionId(transaction.getId()), transactionNumber);
                    log.info(message);
                    sendFailedMailAndStopExporting(transaction, message);
                    return;
                }

                if (transaction.getRefundable() != null &&
                        !transaction.getRefundable().equals(transactionService.countAllByTransactionIdAndRefund(transaction.getId(), 1))) {
                    String message = String.format("Refundable article amount %d of transaction does not equal the count of refundable transaction articles %d for transaction number %s",
                            transaction.getTotal(), transactionService.countAllByTransactionIdAndRefund(transaction.getId(), 1), transactionNumber);
                    log.info(message);
                    sendFailedMailAndStopExporting(transaction, message);
                    return;
                }

                if (transaction.getCollected() != null &&
                        !transaction.getCollected().equals(transactionService.countAllByTransactionIdAndCollected(transaction.getId(), 1))) {
                    String message = String.format("Collected article amount %d of transaction does not equal the count of collected transaction articles %d for transaction number %s",
                            transaction.getTotal(), transactionService.countAllByTransactionIdAndCollected(transaction.getId(), 1), transactionNumber);
                    log.info(message);
                    sendFailedMailAndStopExporting(transaction, message);
                    return;
                }

                companyService.findById(transaction.getCompanyId()).ifPresent(company -> {
                    final String companyNumber = company.getNumber();
                    try {
                        if (transaction.getType().equals(ImportHelper.FILE_TYPE_AA_TRANSACTION) || transaction.getType().equals(ImportHelper.FILE_TYPE_TRANSACTION)) {
                            Path acceptedPath = directoryService.getTransactionsAcceptedPath();
                            createTransactionFile(acceptedPath, transaction, company);
                            createTransactionHashFile(acceptedPath, transactionNumber, companyNumber);
                        } else {
                            Path acceptedPath = directoryService.getBagsAcceptedPath();
                            createTransactionFile(acceptedPath, transaction, company);
                            createTransactionHashFile(acceptedPath, transactionNumber, companyNumber);
                        }

                        exportedTransactionService.save(transaction);
                        transactionService.delete(transaction);
                        log.info("Exported database transaction to CSV with transaction number {} and company number {}", transactionNumber, companyNumber);
                    } catch (IOException e) {
                        log.error("An error while creating transaction CSV with transaction number {} and company number {}", transactionNumber, companyNumber, e);
                    }
                });
            });
        } catch (Exception e) {
            log.warn("Queue error for {} is:", transactionExport, e);
        }
    }

    private void createTransactionFile(Path path, Transaction transaction, Company company) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                path.resolve(transaction.getTransactionNumber() + "-" + company.getNumber() + ".csv"),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE)) {

            if (ImportedFileValidationHelper.version17Check(transaction.getVersion())) {
                writeValues(writer, HDR.title,
                        transaction.getVersion(),
                        transaction.getDateTime().format(DATETIMEFORMATTER),
                        transaction.getStoreId(),
                        transaction.getSerialNumber(),
                        ofNullable(transaction.getLabelNumber()).orElse(""), // new in version 0162
                        ofNullable(transaction.getBagType()).orElse(""), // new in version 0162
                        ofNullable(transaction.getCharityNumber()).orElse(""), // new in version 0170
                        transaction.getReceivedDate().format(DATETIMEFORMATTER),
                        transaction.getType());
            } else if (ImportedFileValidationHelper.version162Check(transaction.getVersion())) {
                writeValues(writer, HDR.title,
                        transaction.getVersion(),
                        transaction.getDateTime().format(DATETIMEFORMATTER),
                        transaction.getStoreId(),
                        transaction.getSerialNumber(),
                        ofNullable(transaction.getLabelNumber()).orElse(""), // new in version 0162
                        ofNullable(transaction.getBagType()).orElse(""), // new in version 0162
                        transaction.getReceivedDate().format(DATETIMEFORMATTER),
                        transaction.getType());
            } else {
                writeValues(writer, HDR.title,
                        transaction.getVersion(),
                        transaction.getDateTime().format(DATETIMEFORMATTER),
                        transaction.getStoreId(),
                        transaction.getSerialNumber(),
                        transaction.getReceivedDate().format(DATETIMEFORMATTER),
                        transaction.getType());
            }

            Map<String, List<TransactionArticle>> acceptedArticles = new HashMap<>();
            Map<String, List<TransactionArticle>> declinedArticles = new HashMap<>();
            List<TransactionArticle> transactionArticles = transactionService.findAllByTransactionId(transaction.getId());
            for (TransactionArticle article : transactionArticles) {
                if (article.getRefund() == 1) {
                    acceptedArticles.putIfAbsent(article.getArticleNumber(), new ArrayList<>());
                    acceptedArticles.get(article.getArticleNumber()).add(article);
                } else {
                    declinedArticles.putIfAbsent(article.getArticleNumber(), new ArrayList<>());
                    declinedArticles.get(article.getArticleNumber()).add(article);
                }
            }

            writeArticles(writer, acceptedArticles, transaction);
            writeArticles(writer, declinedArticles, transaction);

            if (ImportedFileValidationHelper.version162Check(transaction.getVersion())) {
                writeValues(writer, SUM.title,
                        transaction.getTotal().toString(),
                        transaction.getRefundable().toString(),
                        transaction.getCollected().toString(),
                        ofNullable(transaction.getManual())
                                .map(Object::toString)
                                .orElse(""), // new in version 0162
                        ofNullable(transaction.getRejected())
                                .map(Object::toString)
                                .orElse("")); // new in version 0162
            } else {
                writeValues(writer, SUM.title,
                        transaction.getTotal().toString(),
                        transaction.getRefundable().toString(),
                        transaction.getCollected().toString());
            }
        }
    }

    private void writeArticles(BufferedWriter writer, Map<String, List<TransactionArticle>> articles, Transaction transaction) throws IOException {
        for (Map.Entry<String, List<TransactionArticle>> entry : articles.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    TransactionArticle article = entry.getValue().get(0);
                    int articleCount = entry.getValue().size();
                    if (ImportedFileValidationHelper.version162Check(transaction.getVersion())) {
                        writeValues(writer, POS.title,
                                article.getArticleNumber(),
                                ofNullable(article.getScannedWeight())
                                        .map(Object::toString)
                                        .orElse(""),
                                article.getMaterial().toString(), // new in version 015
                                article.getRefund().toString(),
                                article.getCollected().toString(),
                                ofNullable(article.getManual())
                                        .map(Object::toString)
                                        .orElse(""), // new in version 0162
                                Integer.toString(articleCount));
                    } else if (ImportedFileValidationHelper.version15Check(transaction.getVersion())) {
                        writeValues(writer, POS.title,
                                article.getArticleNumber(),
                                ofNullable(article.getScannedWeight())
                                        .map(Object::toString)
                                        .orElse(""),
                                article.getMaterial().toString(), // new in version 015
                                article.getRefund().toString(),
                                article.getCollected().toString(),
                                Integer.toString(articleCount));
                    } else {
                        writeValues(writer, POS.title,
                                article.getArticleNumber(),
                                ofNullable(article.getScannedWeight())
                                        .map(Object::toString)
                                        .orElse(""),
                                article.getRefund().toString(),
                                article.getCollected().toString(),
                                Integer.toString(articleCount));
                    }
                }
            }
    }

    private void createTransactionHashFile(Path path, String transactionNumber, String companyNumber) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path.resolve(transactionNumber + "-" + companyNumber + ".hash"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writeValues(writer, false, FileUtils.getSha256HexFromFile(path.resolve(transactionNumber + "-" + companyNumber + ".csv")));
        }
    }

    private void sendFailedMailAndStopExporting(Transaction transaction, String message) {
        Map<String, String> env = System.getenv();
        String containerIdPart = env.get("TASK_ID") != null && env.get("TASK_ID").length() > 7 ? " " + env.get("TASK_ID").substring(0, 7) : "";

        String subject = String.format("Transaction export of transaction number %s failed on server %s", transaction.getTransactionNumber(), applicationName + containerIdPart);
        final MailData mailData = new MailData(subject).addTo(fileExportFailedMailTo).html(true);
        StringBuilder text = new StringBuilder(message).append("\n\n");
        mailData.text(text.toString());
        mailData.emailType(BaseMessageType.SYSTEM);

        mailService.sendMail(mailData, null);

        transaction.setFailed(true);
        transaction.setInQueue(false);
        transactionService.save(transaction);
    }
}
