package com.tible.ocm.jobs;

import com.tible.hawk.core.models.BaseTask;
import com.tible.ocm.models.CommunicationType;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.rabbitmq.PublisherTransactionCompanyConfirmed;
import com.tible.ocm.rabbitmq.TransactionCompanyConfirmedPayload;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.DirectoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Test for {@link ConfirmedFilesExporterTask}
 */
@ExtendWith(MockitoExtension.class)
class ConfirmedFilesExporterTaskTests extends BaseJobTest {

    @InjectMocks
    private ConfirmedFilesExporterTask confirmedFilesExporterTask;

    private CompanyService companyService;
    private PublisherTransactionCompanyConfirmed publisher;

    @Captor
    private ArgumentCaptor<TransactionCompanyConfirmedPayload> payloadArgCaptor;

    private Company company;

    @BeforeEach
    void setUp() {
        /* For constructor and field injection at the same time (CommonTask class)
        https://stackoverflow.com/a/51305235 */
        setUpMocks();
        setUpTask(confirmedFilesExporterTask.getTaskName());

        setUpMockedData();
    }

    @Override
    protected void setUpMocks() {
        super.setUpMocks();

        companyService = Mockito.mock(CompanyService.class);
        DirectoryService directoryService = Mockito.mock(DirectoryService.class);
        publisher = Mockito.mock(PublisherTransactionCompanyConfirmed.class);

        confirmedFilesExporterTask = new ConfirmedFilesExporterTask(
                taskService, settingsService, baseMailService, consulClient,
                companyService, directoryService, publisher
        );
        MockitoAnnotations.openMocks(this);
    }

    private void setUpMockedData() {
        company = new Company();
        company.setId("1");
        company.setIpAddress("192.168.9.10");
        company.setCommunication(CommunicationType.SFTP);
    }

    /**
     * {@link ConfirmedFilesExporterTask#toExecute(BaseTask)}
     */
    @Test
    void shouldReturnEmptyCompaniesListWhenCommunicationTypeIsNotAllowed() {
        //given
        company.setCommunication("test");
        given(companyService.findAll()).willReturn(List.of(company));

        //when
        confirmedFilesExporterTask.receiveMessage(taskMessage);

        //then
        then(publisher).shouldHaveNoInteractions();
    }

    /**
     * {@link ConfirmedFilesExporterTask#toExecute(BaseTask)}
     */
    @Test
    void shouldCreatePayloadAndPublishCompanyToQueue() {
        //given
        given(companyService.findAll()).willReturn(List.of(company));

        //when
        confirmedFilesExporterTask.receiveMessage(taskMessage);

        //then
        then(publisher).should().publishToQueue(payloadArgCaptor.capture());

        TransactionCompanyConfirmedPayload value = payloadArgCaptor.getValue();
        assertEquals(company.getId(), value.getCompanyId());
    }
}
