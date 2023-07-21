package com.tible.ocm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tible.hawk.core.controllers.advice.BaseResponseExceptionHandler;
import com.tible.ocm.controllers.OAuthClientController;
import com.tible.ocm.dto.OAuthClientDto;
import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.services.OAuthClientService;
import org.apache.commons.compress.utils.Lists;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.persistence.EntityNotFoundException;

import static java.util.Optional.of;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles(profiles = "tests")
@RunWith(MockitoJUnitRunner.class)
public class OAuthClientControllerUnitTests {

    private static ObjectMapper objectMapper;
    private final static String PATH = "/client";
    private final static OAuthClient OAUTH_CLIENT = createOauthClient("test", "test", "test");

    @Mock
    private OAuthClientService oauthClientService;
    @Mock
    private ConversionService conversionService;

    private MockMvc mockMvc;

    private static OAuthClient createOauthClient(String clientId, String clientSecret, String scope) {
        final OAuthClient oauthClient = new OAuthClient();
        oauthClient.setClientId(clientId);
        oauthClient.setClientSecret(clientSecret);
        oauthClient.setScope(scope);
        oauthClient.setAccessTokenValidity((int) System.currentTimeMillis() / 10000000);
        oauthClient.setRefreshTokenValidity((int) System.currentTimeMillis() / 1000000);
        oauthClient.setRvmMachines(Lists.newArrayList());
        return oauthClient;
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(new OAuthClientController(oauthClientService, conversionService))
                .setControllerAdvice(new BaseResponseExceptionHandler())
                .build();
    }

    @Test
    public void indexPositiveTest() throws Exception {

        final OAuthClientDto detail = OAuthClientDto.from(OAUTH_CLIENT);

        when(oauthClientService.findByClientId(OAUTH_CLIENT.getClientId())).thenReturn(of(OAUTH_CLIENT));

        this.mockMvc.perform(get(PATH + "/{clientId}", OAUTH_CLIENT.getClientId()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.clientId").value(is(detail.getClientId()), String.class))
                .andExpect(jsonPath("$.clientSecret", Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.scope", hasSize(detail.getScope().size())))
                .andExpect(jsonPath("$.scope", Matchers.containsInAnyOrder(detail.getScope().toArray())))
                .andExpect(jsonPath("$.accessTokenValidity").value(is(detail.getAccessTokenValidity()), Integer.class))
                .andExpect(jsonPath("$.refreshTokenValidity").value(is(detail.getRefreshTokenValidity()), Integer.class));
    }

    @Test
    public void indexNotFoundTest() throws Exception {
        when(oauthClientService.findByClientId(OAUTH_CLIENT.getClientId())).thenThrow(EntityNotFoundException.class);

        this.mockMvc.perform(get(PATH + "/{clientId}", OAUTH_CLIENT.getClientId()))
                .andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    public void savePositiveTest() throws Exception {

        when(conversionService.convert(isNotNull(), isNotNull())).thenReturn(OAUTH_CLIENT);
        when(oauthClientService.save(isA(OAuthClient.class))).thenReturn(OAUTH_CLIENT);

        final String json = getObjectMapper().writeValueAsString(OAuthClientDto.from(OAUTH_CLIENT));

        this.mockMvc.perform(post(PATH + "/save")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.clientId").value(is(OAUTH_CLIENT.getClientId()), String.class))
                .andExpect(jsonPath("$.clientSecret", Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$.scope", hasSize(OAUTH_CLIENT.getScope().split(",").length)))
                .andExpect(jsonPath("$.scope", Matchers.containsInAnyOrder(OAUTH_CLIENT.getScope().split(","))))
                .andExpect(jsonPath("$.accessTokenValidity").value(is(OAUTH_CLIENT.getAccessTokenValidity()), Integer.class))
                .andExpect(jsonPath("$.refreshTokenValidity").value(is(OAUTH_CLIENT.getRefreshTokenValidity()), Integer.class));
    }

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = Jackson2ObjectMapperBuilder.json()
                    .modules(new JavaTimeModule())
                    .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                    .build();
        }
        return objectMapper;
    }

}
