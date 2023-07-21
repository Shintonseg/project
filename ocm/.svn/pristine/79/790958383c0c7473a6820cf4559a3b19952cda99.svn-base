package com.tible.ocm.repositories.mongo;

import com.tible.ocm.models.mongo.SynchronizedDirectory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SynchronizedDirectoryRepository extends MongoRepository<SynchronizedDirectory, String> {

    boolean existsByName(String name);

    SynchronizedDirectory findByName(String name);
}
