package com.tible.ocm.rabbitmq;

import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.utils.OcmFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tible.ocm.utils.ImportHelper.*;

@Slf4j
@Component
public class ListenerTransactionCompanyConfirmed {

    private final DirectoryService directoryService;
    private final CompanyService companyService;

    ListenerTransactionCompanyConfirmed(DirectoryService directoryService,
                                        CompanyService companyService) {
        this.directoryService = directoryService;
        this.companyService = companyService;
    }

    @RabbitListener(bindings = {
            @QueueBinding(value = @Queue(value = "TransactionCompanyConfirmed"), key = "transactionCompanyConfirmed", exchange = @Exchange(value = "${exchange.topic}", type = ExchangeTypes.TOPIC, durable = "true"))})
    public void receiveMessage(@Payload final TransactionCompanyConfirmedPayload payload) {

        companyService.findById(payload.getCompanyId()).ifPresent(company -> {

            final Path transactionsConfirmedDir = directoryService.getTransactionsConfirmedPath();
            final Path bagsConfirmedDir = directoryService.getBagsConfirmedPath();

            Path transactionsCompanyConfirmedPath = transactionsConfirmedDir.resolve(company.getIpAddress());
            Path bagsCompanyConfirmedPath = bagsConfirmedDir.resolve(company.getIpAddress());

            if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(transactionsCompanyConfirmedPath)) {
                log.warn("Creating transactions confirmed company directory failed");
                return;
            }

            if (!OcmFileUtils.checkOrCreateDirWithFullPermissions(bagsCompanyConfirmedPath)) {
                log.warn("Creating bags confirmed company directory failed");
                return;
            }

            Path companyIpDirectoryConfirmedPath = extractConfirmedPathFromCompany(company);
            moveFilesToCompanyIpDirectory(transactionsCompanyConfirmedPath, companyIpDirectoryConfirmedPath);
            moveFilesToCompanyIpDirectory(bagsCompanyConfirmedPath, companyIpDirectoryConfirmedPath);
        });

    }

    private Path extractConfirmedPathFromCompany(Company company) {
        Path companyPath = directoryService.getRoot().resolve(company.getIpAddress());
        Path companyBagsPath = companyPath.resolve(TRANS_DIRECTORY); // Yeah, bags is using TRANS directory at the ip directory side.
        OcmFileUtils.checkOrCreateDirWithFullPermissions(companyBagsPath);
        Path confirmedPathFromCompany = companyBagsPath.resolve(CONFIRMED_DIRECTORY);
        OcmFileUtils.checkOrCreateDirWithFullPermissions(confirmedPathFromCompany);
        return confirmedPathFromCompany;
    }

    private void moveFilesToCompanyIpDirectory(Path from, Path to) {
        try (Stream<Path> paths = Files.find(from, 1, (path, attributes) -> {
            String fileName = path.getFileName().toString();
            return !attributes.isDirectory() && fileName.endsWith(CONFIRMED_FILE_FORMAT);
        })) {
            paths.collect(Collectors.toList()).forEach(confirmedFilePath -> {
                final Path parent = confirmedFilePath.getParent();
                String fileNameBase = getFilename(confirmedFilePath);
                copyIfExists(to, parent.resolve(fileNameBase + CONFIRMED_FILE_FORMAT), parent.resolve(fileNameBase + HASH_FILE_FORMAT));
                // log.info("Moved {} confirmed bag/transaction files to company ip directory {}", fileNameBase, to.getParent().getFileName().toString());
            });
        } catch (IOException e) {
            log.warn("Moving confirmed bags/transactions files error: {}", e.getMessage());
        }
    }
}
