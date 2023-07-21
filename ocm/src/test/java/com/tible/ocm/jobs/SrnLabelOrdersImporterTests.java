package com.tible.ocm.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tible.hawk.core.models.BaseTask;
import com.tible.ocm.dto.LabelOrderDto;
import com.tible.ocm.models.mongo.LabelOrder;
import com.tible.ocm.services.DirectoryService;
import com.tible.ocm.services.LabelOrderService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Test for {@link SrnLabelOrdersImporter}
 */
@ExtendWith(MockitoExtension.class)
class SrnLabelOrdersImporterTests extends BaseJobTest {

    private static final String LABEL_ORDERS_EXPORT_DIR = "labelOrders";
    private static final String FILE_NAME = "test.json";
    private static final Path LABEL_ORDERS_PATH = Path.of(LABEL_ORDERS_EXPORT_DIR);
    private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.now();

    private DirectoryService directoryService;
    private ConversionService conversionService;
    private ObjectMapper objectMapper;
    private LabelOrderService labelOrderService;

    @InjectMocks
    private SrnLabelOrdersImporter labelOrdersImporter;

    @Captor
    private ArgumentCaptor<LabelOrder> labelOrderCaptor;

    private MockedStatic<Files> filesMock;
    private MockedStatic<FileUtils> fileUtilsMock;
    private BufferedReader bufferedReaderMock;
    private LabelOrderDto labelDto;
    private LabelOrder labelOrder;
    private LabelOrderDto[] labelOrderArray;

    @BeforeEach
    void setUp() {
        setUpMocks();

        setUpTask(labelOrdersImporter.getTaskName());

        setUpMockedData();
    }

    @Override
    protected void setUpMocks() {
        super.setUpMocks();
        directoryService = Mockito.mock(DirectoryService.class);
        conversionService = Mockito.mock(ConversionService.class);
        objectMapper = Mockito.mock(ObjectMapper.class);
        labelOrderService = Mockito.mock(LabelOrderService.class);

        labelOrdersImporter = new SrnLabelOrdersImporter(
                taskService, settingsService, baseMailService, consulClient,
                directoryService, conversionService, objectMapper, labelOrderService
        );
        MockitoAnnotations.openMocks(this);
        filesMock = Mockito.mockStatic(Files.class);
        fileUtilsMock = Mockito.mockStatic(FileUtils.class);
        bufferedReaderMock = Mockito.mock(BufferedReader.class);
    }

    private void setUpMockedData() {
        labelDto = new LabelOrderDto();
        labelDto.setId("1");

        labelOrder = new LabelOrder();
        labelOrder.setId("1");
        labelOrder.setCustomerNumber("test");
        labelOrder.setRvmOwnerNumber("test");
        labelOrder.setFirstLabelNumber(1L);
        labelOrder.setCustomerLocalizationNumber("test");
        labelOrder.setBalance(1L);
        labelOrder.setOrderDate(LOCAL_DATE_TIME);
        labelOrder.setMarkAllLabelsAsUsed(true);

        labelOrderArray = new LabelOrderDto[]{labelDto};
    }

    @AfterEach
    void cleanUp() {
        filesMock.close();
        fileUtilsMock.close();
    }

    /**
     * {@link SrnLabelOrdersImporter#toExecute(BaseTask)}
     */
    @Test
    void shouldSaveExistingLabelOrder() throws IOException {
        //given
        given(directoryService.getLabelOrdersPath()).willReturn(LABEL_ORDERS_PATH);
        filesMock.when(() -> Files.find(any(), anyInt(), any()))
                .thenReturn(Stream.of(LABEL_ORDERS_PATH.resolve(FILE_NAME)));

        given(labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(any(), any(), any()))
                .willReturn(true);
        given(labelOrderService.findByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(any(), any(), any()))
                .willReturn(labelOrder);
        given(labelOrderService.save(labelOrder)).willReturn(labelOrder);

        givenReadLabelOrders();

        //when
        labelOrdersImporter.receiveMessage(taskMessage);

        //then
        then(labelOrderService).should().save(labelOrderCaptor.capture());

        LabelOrder actualOrder = labelOrderCaptor.getValue();
        assertEquals(labelOrder.getCustomerLocalizationNumber(), actualOrder.getCustomerLocalizationNumber());
        assertEquals(labelOrder.getBalance(), actualOrder.getBalance());
        assertEquals(labelOrder.getOrderDate(), actualOrder.getOrderDate());
        assertEquals(labelOrder.getMarkAllLabelsAsUsed(), actualOrder.getMarkAllLabelsAsUsed());
    }

    /**
     * {@link SrnLabelOrdersImporter#toExecute(BaseTask)}
     */
    @Test
    void shouldSaveNewLabelOrder() throws IOException {
        //given
        given(directoryService.getLabelOrdersPath())
                .willReturn(LABEL_ORDERS_PATH);
        filesMock.when(() -> Files.find(any(), anyInt(), any()))
                .thenReturn(Stream.of(LABEL_ORDERS_PATH.resolve(FILE_NAME)));
        given(labelOrderService.existsByCustomerNumberAndRvmOwnerNumberAndFirstLabelNumber(any(), any(), any()))
                .willReturn(false);
        given(labelOrderService.save(labelOrder)).willReturn(labelOrder);

        givenReadLabelOrders();

        //when
        labelOrdersImporter.receiveMessage(taskMessage);

        //then
        then(labelOrderService).should().save(labelOrderCaptor.capture());

        LabelOrder actualOrder = labelOrderCaptor.getValue();
        assertEquals(labelOrder.getCustomerLocalizationNumber(), actualOrder.getCustomerLocalizationNumber());
        assertEquals(labelOrder.getBalance(), actualOrder.getBalance());
        assertEquals(labelOrder.getOrderDate(), actualOrder.getOrderDate());
        assertEquals(labelOrder.getMarkAllLabelsAsUsed(), actualOrder.getMarkAllLabelsAsUsed());
    }

    private void givenReadLabelOrders() throws IOException {
        filesMock.when(() -> Files.newBufferedReader(any(), eq(StandardCharsets.UTF_8))).thenReturn(bufferedReaderMock);
        given(objectMapper.readValue(bufferedReaderMock, LabelOrderDto[].class))
                .willReturn(labelOrderArray);
        given(conversionService.convert(labelDto, LabelOrder.class))
                .willReturn(labelOrder);
    }
}
