package com.tible.ocm.controllers;

import com.tible.ocm.models.CharitiesResponse;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.OAuthClientService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/charity")
@PreAuthorize("#oauth2.hasScope('ocm') or #oauth2.hasScope('tible')")
public class CharityController {

    private final CompanyService companyService;
    private final OAuthClientService oauthClientService;

    public CharityController(CompanyService companyService,
                             OAuthClientService oauthClientService) {
        this.companyService = companyService;
        this.oauthClientService = oauthClientService;
    }

    @GetMapping("/export")
    public CharitiesResponse getAllCharities(OAuth2Authentication auth) {
        String clientId = auth.getOAuth2Request().getClientId();
        String version = oauthClientService.getVersion(auth);
        if (version == null) {
            return null;
        }

        if (clientId.equals("tible-admin") || ImportedFileValidationHelper.version17Check(version)) {
            return companyService.getAllCharities(version);
        } else {
            return null;
        }
    }
}
