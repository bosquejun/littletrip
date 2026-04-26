package com.littletrip.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int requests() default 100;
    TimeUnit per() default TimeUnit.MINUTES;
    /** Request parameter names to use as the rate limit key instead of the API key. */
    String[] keyParams() default {};
}
