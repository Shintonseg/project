package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.OAuthClientDto;
import com.tible.ocm.models.mongo.OAuthClient;
import org.springframework.core.convert.converter.Converter;

public interface OAuthClientConverter extends Converter<OAuthClientDto, OAuthClient> {
}
