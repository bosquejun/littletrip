package com.littletrip.api.config;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.service.RateLimitService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;

    public RateLimitInterceptor(RateLimitService rateLimitService, MeterRegistry meterRegistry) {
        this.rateLimitService = rateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;

        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        if (annotation == null) return true;
        // Methods with keyParams are handled by RateLimitAspect (needs parsed request body)
        if (annotation.keyParams().length > 0) return true;

        String clientId = resolveClientId(request, annotation);

        String endpoint = handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
        String key = "ratelimit:" + endpoint + ":" + clientId;

        Duration window = Duration.of(1, annotation.per().toChronoUnit());

        if (rateLimitService.tryConsume(key, annotation.requests(), window)) {
            long remaining = rateLimitService.getRemainingRequests(key, annotation.requests(), window);
            long reset = rateLimitService.getResetTime(window);
            response.setHeader("X-RateLimit-Limit", String.valueOf(annotation.requests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(reset));
            recordMetric(endpoint, "allowed");
            return true;
        }

        recordMetric(endpoint, "throttled");
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
        response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + window.toSeconds() + "}");
        return false;
    }

    private String resolveClientId(HttpServletRequest request, RateLimit annotation) {
        if (annotation.keyParams().length > 0) {
            StringBuilder sb = new StringBuilder();
            for (String param : annotation.keyParams()) {
                String value = request.getParameter(param);
                if (value == null) value = "unknown";
                if (!sb.isEmpty()) sb.append(":");
                sb.append(value);
            }
            return sb.toString();
        }
        String apiKey = (String) request.getAttribute("apiKey");
        return apiKey != null ? apiKey : request.getRemoteAddr();
    }

    private void recordMetric(String endpoint, String result) {
        Counter.builder("api.rate.limit.requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }
}
