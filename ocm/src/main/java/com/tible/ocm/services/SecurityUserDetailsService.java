package com.tible.ocm.services;

import com.tible.hawk.core.models.BaseUser;
import com.tible.hawk.core.services.BaseUserService;
import com.tible.ocm.models.SecurityUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final BaseUserService userService;

    public SecurityUserDetailsService(BaseUserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final BaseUser user = userService.findByName(username);

        if (user == null) {
            throw new UsernameNotFoundException("Not found a user for the name " + username);
        }

        return new SecurityUser(user.getName(), user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
    }
}
