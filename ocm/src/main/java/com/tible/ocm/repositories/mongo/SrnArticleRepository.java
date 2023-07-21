package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.SrnArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SrnArticleRepository extends MongoRepository<SrnArticle, String> {

    boolean existsByNumber(String articleNumber);

    SrnArticle findByNumber(String number);

    List<SrnArticle> findAllByMaterialIn(Collection<Integer> material);
}
