package com.littletrip.api.config;

import com.littletrip.api.annotation.RateLimit;
import com.littletrip.api.service.RateLimitService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    private RateLimitInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(rateLimitService, new SimpleMeterRegistry());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void preHandle_noAnnotation_allowsRequest() throws Exception {
        HandlerMethod handler = mock(HandlerMethod.class);
        when(handler.getMethodAnnotation(RateLimit.class)).thenReturn(null);
        when(handler.getBeanType()).thenReturn((Class) Object.class);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void preHandle_withinLimit_allowsRequest() throws Exception {
        HandlerMethod handler = mockHandlerWithAnnotation(100, TimeUnit.MINUTES);
        when(rateLimitService.tryConsume(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(rateLimitService.getRemainingRequests(anyString(), anyInt(), any(Duration.class))).thenReturn(99L);
        when(rateLimitService.getResetTime(any(Duration.class))).thenReturn(9999L);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
    }

    @Test
    void preHandle_overLimit_returns429() throws Exception {
        HandlerMethod handler = mockHandlerWithAnnotation(10, TimeUnit.MINUTES);
        when(rateLimitService.tryConsume(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, handler);

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void preHandle_nonHandlerMethod_allowsRequest() throws Exception {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    private HandlerMethod mockHandlerWithAnnotation(int requests, TimeUnit per) {
        RateLimit annotation = mock(RateLimit.class);
        when(annotation.requests()).thenReturn(requests);
        when(annotation.per()).thenReturn(per);
        when(annotation.keyParams()).thenReturn(new String[0]);

        HandlerMethod handler = mock(HandlerMethod.class);
        when(handler.getMethodAnnotation(RateLimit.class)).thenReturn(annotation);
        when(handler.getBeanType()).thenReturn((Class) Object.class);
        when(handler.getMethod()).thenReturn(RateLimitInterceptorTest.class.getMethods()[0]);

        return handler;
    }
}
