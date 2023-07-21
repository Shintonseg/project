package com.tible.ocm.services.impl;

import com.tible.ocm.models.mongo.SynchronizedDirectory;
import com.tible.ocm.repositories.mongo.SynchronizedDirectoryRepository;
import com.tible.ocm.services.SynchronizedDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
public class SynchronizedDirectoryServiceImpl implements SynchronizedDirectoryService {

    private final SynchronizedDirectoryRepository synchronizedDirectoryRepository;

    public SynchronizedDirectoryServiceImpl(SynchronizedDirectoryRepository synchronizedDirectoryRepository) {
        this.synchronizedDirectoryRepository = synchronizedDirectoryRepository;
    }

    @Override
    public boolean existsByName(String name) {
        return synchronizedDirectoryRepository.existsByName(name);
    }

    @Override
    public SynchronizedDirectory findByName(String name) {
        return synchronizedDirectoryRepository.findByName(name);
    }

    @Override
    public SynchronizedDirectory save(SynchronizedDirectory synchronizedDirectory) {
        return synchronizedDirectoryRepository.save(synchronizedDirectory);
    }
}
