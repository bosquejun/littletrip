package com.littletrip.api.security;

import com.littletrip.api.model.ApiKey;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final ApiKey apiKey;

    public ApiKeyAuthentication(ApiKey apiKey) {
        super(List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
        this.apiKey = apiKey;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return apiKey;
    }
}
