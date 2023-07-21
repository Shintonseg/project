package com.tible.ocm.services.impl;

import com.tible.ocm.repositories.mongo.ExistingTransactionLatestRepository;
import com.tible.ocm.repositories.mysql.ExistingTransactionRepository;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ExistingTransactionServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
class ExistingTransactionServiceImplTest {

    @InjectMocks
    private ExistingTransactionServiceImpl existingTransactionServiceImpl;

    @Mock
    private ExistingTransactionLatestRepository existingTransactionLatestRepository;

    @Mock
    private ExistingTransactionRepository existingTransactionRepository;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * {@link ExistingTransactionServiceImpl#lazyCheckIsTransactionAlreadyExists(String, String)}
     */
    @Test
    void shouldReturnTrueWhenTransactionFoundAndNotCallSecondMethod() {
        ExistingTransactionServiceImpl spyExistingTransactionServiceImpl = spy(existingTransactionServiceImpl);

        //given
        given(existingTransactionLatestRepository.existsByNumberAndRvmOwnerNumber(anyString(), anyString())).willReturn(true);

        //when
        boolean isExist = spyExistingTransactionServiceImpl.lazyCheckIsTransactionAlreadyExists(anyString(), anyString());

        //then
        verify(spyExistingTransactionServiceImpl, never()).existsByTransactionNumberAndRvmOwnerNumber(anyString(), anyString());
        assertTrue(isExist);
    }

    /**
     * {@link ExistingTransactionServiceImpl#lazyCheckIsTransactionAlreadyExists(String, String)}
     */
    @Test
    void shouldReturnTrueWhenTransactionFoundAndCallSecondMethod() {
        ExistingTransactionServiceImpl spyExistingTransactionServiceImpl = spy(existingTransactionServiceImpl);

        //given
        given(existingTransactionLatestRepository.existsByNumberAndRvmOwnerNumber(anyString(), anyString())).willReturn(false);
        given(spyExistingTransactionServiceImpl.existsByTransactionNumberAndRvmOwnerNumber(anyString(), anyString())).willReturn(true);

        //when
        boolean isExist = spyExistingTransactionServiceImpl.lazyCheckIsTransactionAlreadyExists(anyString(), anyString());

        //then
        verify(spyExistingTransactionServiceImpl, atLeastOnce()).existsByTransactionNumberAndRvmOwnerNumber(anyString(), anyString());
        assertTrue(isExist);
    }

    /**
     * {@link ExistingTransactionServiceImpl#lazyCheckIsTransactionAlreadyExists(String, String)}
     */
    @Test
    void shouldReturnFalseWhenTransactionNotFound() {
        ExistingTransactionServiceImpl spyExistingTransactionService = spy(existingTransactionServiceImpl);
        //given
        given(existingTransactionLatestRepository.existsByNumberAndRvmOwnerNumber(anyString(), anyString())).willReturn(false);
        given(spyExistingTransactionService.existsByTransactionNumberAndRvmOwnerNumber(anyString(), anyString())).willReturn(false);

        //when
        boolean isExist = spyExistingTransactionService.lazyCheckIsTransactionAlreadyExists(anyString(), anyString());

        //then
        assertFalse(isExist);
        verify(spyExistingTransactionService, atLeastOnce()).existsByTransactionNumberAndRvmOwnerNumber(anyString(), anyString());
    }
}
