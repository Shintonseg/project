package com.tible.ocm.controllers;

import com.tible.ocm.dto.CompanyDto;
import com.tible.ocm.models.CharitiesResponse;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.OAuthClientService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/company")
@PreAuthorize("#oauth2.hasScope('ocm') or #oauth2.hasScope('tible')")
public class CompanyController {

    private final CompanyService companyService;
    private final ConversionService conversionService;
    private final OAuthClientService oauthClientService;

    public CompanyController(CompanyService companyService,
                             ConversionService conversionService,
                             OAuthClientService oauthClientService) {
        this.companyService = companyService;
        this.conversionService = conversionService;
        this.oauthClientService = oauthClientService;
    }

    @GetMapping("/list")
    public List<CompanyDto> list() {
        return companyService.findAll().stream().map(CompanyDto::from).collect(Collectors.toList());
    }

    @PostMapping("/save")
    public CompanyDto saveRvmSupplier(@RequestBody @Valid CompanyDto company) {
        return CompanyDto.from(companyService.save(conversionService.convert(company, Company.class)));
    }

    @GetMapping("/delete")
    public void deleteRvmSupplier(@RequestParam("id") String id) {
        companyService.delete(id);
    }

    @GetMapping("/charities")
    public CharitiesResponse getAllCharities(OAuth2Authentication auth) {
        String clientId = auth.getOAuth2Request().getClientId();
        String version = getVersion(auth);
        if (clientId.equals("tible-admin") || ImportedFileValidationHelper.version17Check(version)) {
            return companyService.getAllCharities(version);
        } else {
            return null;
        }
    }

    private String getVersion(OAuth2Authentication auth) {
        return oauthClientService
                .findByClientId(auth.getOAuth2Request().getClientId())
                .map(OAuthClient::getVersion)
                .orElse("0170");
    }
}
