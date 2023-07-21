package com.tible.ocm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.tible.hawk.core.HawkCore;
import com.tible.hawk.core.configurations.Finder;
import com.tible.ocm.configurations.converters.*;
import com.tible.ocm.configurations.interceptor.OcmScopeRequestIpProtectionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Import({HawkCore.class})
@EnableMongoRepositories("com.tible.ocm.repositories")
@EntityScan({"com.tible.ocm.models"})
@EnableAsync
@EnableScheduling
@EnableJpaRepositories("com.tible.ocm.repositories.mysql")
@SpringBootApplication
public class OcmApplication implements WebMvcConfigurer {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Finder finder;

    @Autowired
    private OcmScopeRequestIpProtectionInterceptor ocmScopeRequestIpProtectionInterceptor;

    public static void main(String[] args) {
        SpringApplication.run(OcmApplication.class, args);
    }

    private static LocalDateTime convertToDateTime(String source) {
        return LocalDateTime.parse(source);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter((DateTimeConverter) source -> Strings.isNullOrEmpty(source) ? null : convertToDateTime(source));
        registry.addConverter((DateConverter) source -> Strings.isNullOrEmpty(source) ? null : convertToDateTime(source).toLocalDate());
        registry.addConverter((OAuthClientConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((SrnArticleConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((RefundArticleConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((TransactionConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((TransactionArticleConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((RvmSupplierConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((RvmMachineConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((CompanyConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((ExistingTransactionConverter) source -> source.toEntity(finder));
        registry.addConverter((ExistingBagConverter) source -> source.toEntity(finder));
        registry.addConverter((ImporterRuleConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((LabelOrderConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((ImporterRuleLimitationsConverter) source -> source.toEntity(mongoTemplate));
        registry.addConverter((RejectedTransactionConverter) source -> source.toEntity(mongoTemplate));
    }

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime dateTime, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                Instant instant = dateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(dateTime));
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(instant));
            }
        });
        builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                TextNode node = jp.getCodec().readTree(jp);
                return convertToDateTime(node.textValue());
            }
        });
        builder.serializerByType(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public void serialize(LocalDate dateTime, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                Instant instant = dateTime.atStartOfDay(ZoneId.systemDefault()).toInstant();
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(instant));
            }
        });
        builder.deserializerByType(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                TextNode node = jp.getCodec().readTree(jp);
                return convertToDateTime(node.textValue()).toLocalDate();
            }
        });
        builder.serializerByType(LocalTime.class, new JsonSerializer<LocalTime>() {
            @Override
            public void serialize(LocalTime time, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                LocalDateTime dateTime = LocalDateTime.of(LocalDate.now(), time);
                Instant instant = dateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(dateTime));
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(instant));
            }
        });
        builder.deserializerByType(LocalTime.class, new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
                TextNode node = jp.getCodec().readTree(jp);
                return convertToDateTime(node.textValue()).toLocalTime();
            }
        });
        return builder;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        ArrayList<String> excludePathPatternsList = new ArrayList<>();
        excludePathPatternsList.add("/oauth/**");
        excludePathPatternsList.add("/article/export");
        excludePathPatternsList.add("/article/refund/list");
        excludePathPatternsList.add("/rvm/**");
        excludePathPatternsList.add("/client/**");
        excludePathPatternsList.add("/company/**");
        excludePathPatternsList.add("/information/**");

        registry.addInterceptor(ocmScopeRequestIpProtectionInterceptor)
                .excludePathPatterns(excludePathPatternsList);
    }

}
