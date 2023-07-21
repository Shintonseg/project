package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.ExistingTransactionDto;
import com.tible.ocm.models.mysql.ExistingTransaction;
import org.springframework.core.convert.converter.Converter;

public interface ExistingTransactionConverter extends Converter<ExistingTransactionDto, ExistingTransaction> {
}
