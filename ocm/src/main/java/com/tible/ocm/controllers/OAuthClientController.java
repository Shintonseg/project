package com.tible.ocm.controllers;

import com.tible.ocm.exceptions.ClientNotFoundException;
import com.tible.ocm.dto.OAuthClientDto;
import com.tible.ocm.models.mongo.OAuthClient;
import com.tible.ocm.services.OAuthClientService;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/client")
@PreAuthorize("#oauth2.hasScope('tible')")
public class OAuthClientController {

    private final OAuthClientService oauthClientService;
    private final ConversionService conversionService;

    public OAuthClientController(OAuthClientService oAuthClientService,
                                 ConversionService conversionService) {
        this.oauthClientService = oAuthClientService;
        this.conversionService = conversionService;
    }

    @GetMapping("/{clientId}")
    public OAuthClientDto findByClientId(@PathVariable String clientId) {
        return OAuthClientDto.from(oauthClientService.findByClientId(clientId).orElse(null));
    }

    @GetMapping("/list")
    public List<OAuthClientDto> list() {
        return oauthClientService.findAll().stream().map(OAuthClientDto::from).collect(Collectors.toList());
    }

    @PostMapping("/save")
    public OAuthClientDto save(@RequestBody @Valid OAuthClientDto oauthClientDto) {
        if (oauthClientDto == null) throw new RuntimeException("Specify data to save.");
        return OAuthClientDto.from(oauthClientService.save(convertTo(oauthClientDto)));
    }

    @GetMapping("/check-exist-client-id")
    public boolean checkExistClientId(@RequestParam(name = "id", required = false) OAuthClient oauthClient,
                                      @RequestParam("client-id") String clientId) {
        return oauthClientService.checkExistClientId(oauthClient, clientId);
    }

    @GetMapping("/clients")
    public List<OAuthClientDto> getClients(@RequestParam(name = "clientIds") List<String> clientIds) {
        return oauthClientService.getClients(clientIds).stream().map(OAuthClientDto::from).collect(Collectors.toList());
    }

    @GetMapping("/remove")
    public void remove(@RequestParam("ids") List<OAuthClient> roles) {
        oauthClientService.remove(roles);
    }

    @GetMapping("/url/{clientId}")
    public String url(@PathVariable String clientId) {
        return oauthClientService.findByClientId(clientId)
                .map(OAuthClient::getUrl)
                .orElseThrow(ClientNotFoundException::new);
    }

    private OAuthClient convertTo(OAuthClientDto oauthClientDto) {
        return conversionService.convert(oauthClientDto, OAuthClient.class);
    }
}
