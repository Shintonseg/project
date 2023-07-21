package com.tible.ocm.configurations;

import com.tible.hawk.core.configurations.*;
import com.tible.hawk.core.models.BaseSettings;
import com.tible.hawk.core.models.BaseUser;
import com.tible.hawk.core.models.BaseUserSettings;
import com.tible.hawk.core.services.BaseSettingsService;
import com.tible.hawk.core.services.BaseUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.ArrayList;

@Configuration
@Order
@EnableWebSecurity
public class SecurityConfigurationImpl extends SecurityConfiguration {

    public SecurityConfigurationImpl(final AbstractLdapUserMapper<BaseUser> ldapUserMapper,
                                     final SessionRegistry sessionRegistry,
                                     final BaseSettingsService<BaseSettings> settingsService,
                                     final LoginAttemptService loginAttemptService,
                                     final AuthenticationEventPublisher authenticationEventPublisher,
                                     final DbAuthenticationProvider dbAuthenticationProvider,
                                     final BaseUserService<BaseUser, BaseUserSettings> userService,
                                     final ApiAuthProvider apiAuthProvider,
                                     final RemoteAddressAttemptService addressAttemptService) {
                super(ldapUserMapper, sessionRegistry, settingsService, loginAttemptService, authenticationEventPublisher,
                dbAuthenticationProvider, apiAuthProvider, userService, addressAttemptService);
    }

    @Override
    public ArrayList<String> ignoreCsrfRequests() {
        return null;
    }

    @Override
    public ArrayList<String> permitRequests() {
        final ArrayList<String> permitRequestList = new ArrayList<>();
        permitRequestList.add("/client/clients");
        permitRequestList.add("/oauth/token");
        permitRequestList.add("/oauth/check_token");
        return permitRequestList;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/*.js", "/*.css");
        web.ignoring().antMatchers("/*.wof*", "/*.ttf", "/*.eot");
        web.ignoring().antMatchers("/*.svg", "/*.png", "/*.jpg");
        super.configure(web);
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
