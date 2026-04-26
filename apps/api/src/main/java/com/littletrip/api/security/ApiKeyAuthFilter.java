package com.littletrip.api.security;

import com.littletrip.api.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(API_KEY_HEADER);
        if (header != null) {
            log.debug("API key header present, looking up key: {}", header.substring(0, Math.min(8, header.length())) + "...");
            var result = apiKeyRepository.findActiveByKeyHash(header);
            if (result.isPresent()) {
                log.debug("Found active API key: {}", result.get().getId());
                SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthentication(result.get()));
            } else {
                log.warn("API key not found in database: {}", header.substring(0, Math.min(8, header.length())) + "...");
            }
        }
        filterChain.doFilter(request, response);
    }
}
