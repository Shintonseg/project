package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.ImporterRuleDto;
import com.tible.ocm.models.mongo.ImporterRule;
import org.springframework.core.convert.converter.Converter;

public interface ImporterRuleConverter extends Converter<ImporterRuleDto, ImporterRule> {

}
