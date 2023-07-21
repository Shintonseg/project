package com.tible.ocm.configurations.converters;

import com.tible.ocm.dto.SrnArticleDto;
import com.tible.ocm.models.mongo.SrnArticle;
import org.springframework.core.convert.converter.Converter;

public interface SrnArticleConverter extends Converter<SrnArticleDto, SrnArticle> {
}
