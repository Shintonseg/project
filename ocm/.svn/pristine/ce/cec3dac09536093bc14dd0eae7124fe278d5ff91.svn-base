package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.SrnRemovedArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SrnRemovedArticleRepository extends MongoRepository<SrnRemovedArticle, String > {

    SrnRemovedArticle findByNumber (String number);
}
