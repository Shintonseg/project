package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.CompanyDto;
import com.tible.ocm.models.mongo.Company;
import org.springframework.core.convert.converter.Converter;

public interface CompanyConverter extends Converter<CompanyDto, Company> {
}
