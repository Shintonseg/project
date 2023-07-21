package com.tible.ocm.services;

import com.tible.ocm.models.CustomerNumbersResponse;
import com.tible.ocm.models.GlnUsageResponse;
import com.tible.ocm.models.LabelIssuedResponse;
import com.tible.ocm.models.LabelUsageResponse;

public interface InformationLookupService {

    LabelUsageResponse getLabelUsage(String rvmOwnerNumber, String number);

    LabelIssuedResponse getLabelIssued(String rvmOwnerNumber, String localizationNumber);

    GlnUsageResponse getGlnUsage(String rvmOwnerNumber, String localizationNumber, int daysInPast);

    CustomerNumbersResponse getCustomerNumbers(String rvmOwnerNumber, String localizationNumber);
}
