package com.tible.ocm.controllers;

import com.tible.hawk.core.utils.Utils;
import com.tible.ocm.dto.RefundArticles;
import com.tible.ocm.dto.SrnArticles;
import com.tible.ocm.models.OcmResponse;
import com.tible.ocm.services.OAuthClientService;
import com.tible.ocm.services.RefundArticleService;
import com.tible.ocm.services.SrnArticleService;
import com.tible.ocm.services.SrnRemovedArticleService;
import com.tible.ocm.utils.ImportedFileValidationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/article")
@PreAuthorize("#oauth2.hasScope('ocm') or #oauth2.hasScope('ah') or #oauth2.hasScope('tible')")
public class ArticleController {

    private final RefundArticleService refundArticleService;
    private final SrnArticleService srnArticleService;
    private final SrnRemovedArticleService srnRemovedArticleService;
    private final OAuthClientService oauthClientService;

    public ArticleController(RefundArticleService refundArticleService,
                             SrnArticleService srnArticleService,
                             SrnRemovedArticleService srnRemovedArticleService,
                             OAuthClientService oauthClientService) {
        this.refundArticleService = refundArticleService;
        this.srnArticleService = srnArticleService;
        this.srnRemovedArticleService = srnRemovedArticleService;
        this.oauthClientService = oauthClientService;
    }

    @PostMapping("/refund")
    public OcmResponse saveRefundArticles(@RequestBody @Valid RefundArticles refundArticles,
                                          HttpServletRequest request) {
        return refundArticleService.saveRefundArticles(refundArticles, Utils.getRemoteAddress(request));
    }

    @GetMapping("/refund/list")
    public RefundArticles list() {
        return refundArticleService.findAll();
    }

    @GetMapping("/export")
    public SrnArticles<?> exportSrnArticles(HttpServletRequest request, OAuth2Authentication auth) {
        //return validateIpAddress(Utils.getRemoteAddress(request), auth.getOAuth2Request().getClientId()) ? srnArticleService.findAll() : null; //TODO: turned this off for now, will be turned back later after the first POC
        String version = oauthClientService.getVersion(auth);
        if (version == null) {
            return null;
        }

        if (ImportedFileValidationHelper.version17Check(version)) {
            return srnArticleService.findAllVersion017(version);
        } else if (ImportedFileValidationHelper.version16Check(version)) {
            return srnArticleService.findAllVersion016(version);
        } else {
            return srnArticleService.findAll(version);
        }
    }

    @GetMapping("/removed")
    public SrnArticles<?> exportRemovedSrnArticles(OAuth2Authentication auth) {
        String version = oauthClientService.getVersion(auth);
        if (version == null) {
            return null;
        }

        return srnRemovedArticleService.findAll(version);
    }
}
