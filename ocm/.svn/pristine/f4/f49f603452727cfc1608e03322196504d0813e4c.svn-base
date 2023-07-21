package com.tible.ocm.configurations.interceptor;

import com.tible.hawk.core.utils.Utils;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.impl.EnvironmentService;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.expression.OAuth2SecurityExpressionMethods;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Component
public class OcmScopeRequestIpProtectionInterceptor extends HandlerInterceptorAdapter {

    private final CompanyService companyService;
    private final EnvironmentService environmentService;

    @Autowired
    public OcmScopeRequestIpProtectionInterceptor(CompanyService companyService,
                                                  EnvironmentService environmentService) {
        this.companyService = companyService;
        this.environmentService = environmentService;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        OAuth2SecurityExpressionMethods oAuth2 = new OAuth2SecurityExpressionMethods(authentication);
        String clientId = authentication.getOAuth2Request().getClientId();

        if ((oAuth2.hasScope("ocm") || oAuth2.hasScope("ah")) && !clientId.equals("tible-admin")) {
            String ipAddress = getIpAddress(req);
            // Locally accept usage of different clientId with different ip.
            if (environmentService.matchGivenProfiles("dev") && ipAddress.equals("127.0.0.1")) {
                return true;
            }

            Company company = companyService.findFirstByIpAddress(ipAddress);
            if (company == null) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
                res.getWriter().print(String.format("{ \"status\":\"declined\", \"messages\": [{\"text\": \"Request is coming from not valid IP %s. ClientId is %s.\"}]}", ipAddress, clientId));
                res.getWriter().close();
                return false;
            } else {
                return true;
            }

            /*boolean isInRange = isIPInRange(clientId, ipAddress, company);

            if (!isInRange) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
                res.getWriter().print(String.format("{ \"errorMsg\": \"Request is coming from not valid IP %s. ClientId is %s.\"}", ipAddress, clientId));
                res.getWriter().close();
                return false;
            }*/
        }
        return super.preHandle(req, res, handler);
    }

    private String getIpAddress(HttpServletRequest req) {
        String ipAddress = Utils.getRemoteAddress(req);
        if (ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }

    private boolean isIPInRange(String clientId, String ipAddress, Company company) {
        if (company == null) {
            return false;
        }

        SubnetUtils subnetUtils = new SubnetUtils(company.getIpRange());
        boolean ifTrunkDisabledPredicate = clientId.equals(ipAddress);
        boolean isInRangePredicate = Arrays.asList(subnetUtils.getInfo().getAllAddresses())
                .contains(ipAddress);
        boolean isTrunkIp = company.isUsingIpTrunking();

        return isTrunkIp ? isInRangePredicate : ifTrunkDisabledPredicate && isInRangePredicate;
    }
}
