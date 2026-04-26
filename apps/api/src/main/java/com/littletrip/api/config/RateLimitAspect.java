package com.littletrip.api.config;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.service.RateLimitService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.time.Duration;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimitService rateLimitService;
    private final MeterRegistry meterRegistry;

    public RateLimitAspect(RateLimitService rateLimitService, MeterRegistry meterRegistry) {
        this.rateLimitService = rateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(rateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (rateLimit.keyParams().length == 0) return joinPoint.proceed();

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return joinPoint.proceed();

        String clientId = extractClientId(joinPoint.getArgs(), rateLimit.keyParams());
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String endpoint = sig.getDeclaringType().getSimpleName() + "." + sig.getMethod().getName();
        String key = "ratelimit:" + endpoint + ":" + clientId;

        Duration window = Duration.of(1, rateLimit.per().toChronoUnit());

        if (rateLimitService.tryConsume(key, rateLimit.requests(), window)) {
            HttpServletResponse response = attrs.getResponse();
            if (response != null) {
                long remaining = rateLimitService.getRemainingRequests(key, rateLimit.requests(), window);
                response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.requests()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitService.getResetTime(window)));
            }
            recordMetric(endpoint, "allowed");
            return joinPoint.proceed();
        }

        recordMetric(endpoint, "throttled");
        HttpServletResponse response = attrs.getResponse();
        if (response != null) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + window.toSeconds() + "}");
        }
        return null;
    }

    private String extractClientId(Object[] args, String[] keyParams) {
        for (Object arg : args) {
            if (arg == null) continue;
            StringBuilder sb = new StringBuilder();
            boolean allFound = true;
            for (String param : keyParams) {
                Object value = getFieldValue(arg, param);
                if (value == null) { allFound = false; break; }
                if (!sb.isEmpty()) sb.append(":");
                sb.append(value);
            }
            if (allFound && !sb.isEmpty()) return sb.toString();
        }
        return "unknown";
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("Field {} not found on {}", fieldName, obj.getClass().getSimpleName());
            return null;
        }
    }

    private void recordMetric(String endpoint, String result) {
        Counter.builder("api.rate.limit.requests")
                .tag("endpoint", endpoint)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }
}
