package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.RvmSupplierDto;
import com.tible.ocm.models.mongo.RvmSupplier;
import org.springframework.core.convert.converter.Converter;

public interface RvmSupplierConverter extends Converter<RvmSupplierDto, RvmSupplier> {
}
