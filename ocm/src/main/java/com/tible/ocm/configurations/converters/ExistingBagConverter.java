package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.ExistingBagDto;
import com.tible.ocm.models.mysql.ExistingBag;
import org.springframework.core.convert.converter.Converter;

public interface ExistingBagConverter extends Converter<ExistingBagDto, ExistingBag> {
}
