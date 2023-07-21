package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.mongo.Transaction;
import org.springframework.core.convert.converter.Converter;

public interface TransactionConverter extends Converter<TransactionDto, Transaction> {
}
