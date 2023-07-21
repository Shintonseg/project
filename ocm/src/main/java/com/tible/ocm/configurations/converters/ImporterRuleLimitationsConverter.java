package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.ImporterRuleLimitationsDto;
import com.tible.ocm.models.mongo.ImporterRuleLimitations;
import org.springframework.core.convert.converter.Converter;

public interface ImporterRuleLimitationsConverter extends Converter<ImporterRuleLimitationsDto, ImporterRuleLimitations> {
}
