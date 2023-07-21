package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.LabelOrderDto;
import com.tible.ocm.models.mongo.LabelOrder;
import org.springframework.core.convert.converter.Converter;

public interface LabelOrderConverter extends Converter<LabelOrderDto, LabelOrder> {
}
