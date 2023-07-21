package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.RejectedTransactionDto;
import com.tible.ocm.models.mongo.RejectedTransaction;
import org.springframework.core.convert.converter.Converter;

public interface RejectedTransactionConverter extends Converter<RejectedTransactionDto, RejectedTransaction> {
}
