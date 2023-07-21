package com.tible.ocm.controllers;

import com.tible.ocm.models.CustomerNumbersResponse;
import com.tible.ocm.models.GlnUsageResponse;
import com.tible.ocm.models.LabelIssuedResponse;
import com.tible.ocm.models.LabelUsageResponse;
import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.services.InformationLookupService;
import com.tible.ocm.services.OAuthClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/information")
@PreAuthorize("#oauth2.hasScope('ah') or #oauth2.hasScope('ocm') or #oauth2.hasScope('tible')")
public class InformationLookupController {

    private final OAuthClientService oauthClientService;
    private final InformationLookupService informationLookupService;

    public InformationLookupController(OAuthClientService oauthClientService,
                                       InformationLookupService informationLookupService) {
        this.oauthClientService = oauthClientService;
        this.informationLookupService = informationLookupService;
    }

    @GetMapping("/label/usage")
    public LabelUsageResponse labelLookup(@RequestParam String number,
                                          OAuth2Authentication auth) {
        return oauthClientService
                .findByClientId(auth.getOAuth2Request().getClientId())
                .map(OAuthClient::getRvmOwnerNumber)
                .map(rvmOwnerNumber -> informationLookupService.getLabelUsage(rvmOwnerNumber, number))
                .orElse(null);
    }

    @GetMapping("/label/issued")
    public LabelIssuedResponse labelIssued(@RequestParam String localizationNumber,
                                           OAuth2Authentication auth) {
        return oauthClientService
                .findByClientId(auth.getOAuth2Request().getClientId())
                .map(OAuthClient::getRvmOwnerNumber)
                .map(rvmOwnerNumber -> informationLookupService.getLabelIssued(rvmOwnerNumber, localizationNumber))
                .orElse(null);
    }

    @GetMapping("/gln")
    public GlnUsageResponse glnLookup(@RequestParam String localizationNumber,
                                      @RequestParam(defaultValue = "7") int daysInPast,
                                      OAuth2Authentication auth) {
        return oauthClientService
                .findByClientId(auth.getOAuth2Request().getClientId())
                .map(OAuthClient::getRvmOwnerNumber)
                .map(rvmOwnerNumber -> informationLookupService.getGlnUsage(rvmOwnerNumber, localizationNumber, daysInPast))
                .orElse(null);
    }

    @GetMapping("/customerNumbers")
    public CustomerNumbersResponse customerNumberLookup(@RequestParam String localizationNumber,
                                                        OAuth2Authentication auth) {
        return oauthClientService
                .findByClientId(auth.getOAuth2Request().getClientId())
                .map(OAuthClient::getRvmOwnerNumber)
                .map(rvmOwnerNumber -> informationLookupService.getCustomerNumbers(rvmOwnerNumber, localizationNumber))
                .orElse(null);
    }
}
