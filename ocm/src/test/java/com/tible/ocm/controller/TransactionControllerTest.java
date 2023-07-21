package com.tible.ocm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tible.ocm.controllers.TransactionController;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.OcmMessage;
import com.tible.ocm.models.OcmStatus;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link TransactionController}
 */
@RunWith(MockitoJUnitRunner.class)
class TransactionControllerTest {

    private static final String PATH = "/transaction";

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;
    private Transaction transaction;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .build();

        setUpMockedData();
    }

    private void setUpMockedData() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        transaction = new Transaction();
        transaction.setId("id");
        transaction.setInQueue(false);
        transaction.setInQueueDateTime(LocalDateTime.now().minusHours(20));
    }

    /**
     * {@link TransactionController#saveTransaction(TransactionDto, HttpServletRequest)}
     */
    @Test
    void shouldSaveTransaction() throws Exception {
        //given
        TransactionDto transactionDto = TransactionDto.from(transaction);
        String jsonTransaction = objectMapper.writeValueAsString(transactionDto);
        OcmTransactionResponse ocmTransactionResponse = new OcmTransactionResponse(
                OcmStatus.ACCEPTED,
                List.of(new OcmMessage("test")),
                "111");
        given(transactionService.handleTransaction(eq(transactionDto), eq("test")))
                .willReturn(ocmTransactionResponse);

        //when
        //then
        this.mockMvc.perform(post(PATH + "/create")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(jsonTransaction))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
