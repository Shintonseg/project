package com.tible.ocm.services.impl;

import com.tible.ocm.repositories.mongo.ExistingBagLatestRepository;
import com.tible.ocm.repositories.mysql.ExistingBagRepository;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Test for {@link ExistingBagServiceImpl}
 */
@ExtendWith(MockitoExtension.class)
class ExistingBagServiceImplTest {

    @InjectMocks
    private ExistingBagServiceImpl existingBagServiceImpl;

    @Mock
    private ExistingBagLatestRepository existingBagLatestRepository;

    @Mock
    private ExistingBagRepository existingBagRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    /**
     * {@link ExistingBagServiceImpl#lazyCheckIsBagAlreadyExists(String)}
     */
    @Test
    void shouldReturnTrueWhenBagFoundAndNotCallSecondMethod() {

        ExistingBagServiceImpl spyExistingBagService = spy(existingBagServiceImpl);
        // given
        given(existingBagLatestRepository.existsByCombinedCustomerNumberLabel(anyString())).willReturn(true);

        // when
        boolean isExist = spyExistingBagService.lazyCheckIsBagAlreadyExists(anyString());

        // then
        Assertions.assertTrue(isExist);
        verify(spyExistingBagService, never()).existsByCombinedCustomerNumberLabel(anyString());
    }

    /**
     * {@link ExistingBagServiceImpl#lazyCheckIsBagAlreadyExists(String)}
     */
    @Test
    void shouldReturnTrueWhenBagFoundAndCallSecondMethod() {
        ExistingBagServiceImpl spyExistingBagService = spy(existingBagServiceImpl);

        // given
        given(existingBagLatestRepository.existsByCombinedCustomerNumberLabel(anyString())).willReturn(false);
        given(spyExistingBagService.existsByCombinedCustomerNumberLabel(anyString())).willReturn(true);

        // when
        boolean isExist = spyExistingBagService.lazyCheckIsBagAlreadyExists(anyString());

        // then
        Assertions.assertTrue(isExist);
        verify(spyExistingBagService, atLeastOnce()).existsByCombinedCustomerNumberLabel(anyString());
    }

    /**
     * {@link ExistingBagServiceImpl#lazyCheckIsBagAlreadyExists(String)}
     */
    @Test
    void shouldReturnFalseWhenBagNotFound() {
        ExistingBagServiceImpl spyExistingBagService = spy(existingBagServiceImpl);

        // given
        given(existingBagLatestRepository.existsByCombinedCustomerNumberLabel(anyString())).willReturn(false);
        given(spyExistingBagService.existsByCombinedCustomerNumberLabel(anyString())).willReturn(false);

        // when
        boolean isExist = spyExistingBagService.lazyCheckIsBagAlreadyExists(anyString());

        // then
        Assertions.assertFalse(isExist);
        verify(spyExistingBagService, atLeastOnce()).existsByCombinedCustomerNumberLabel(anyString());
    }
}
