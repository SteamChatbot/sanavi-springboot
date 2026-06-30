package com.sanavi.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sanavi.backend.common.logging.UserMdcInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class LoggingWebMvcConfig implements WebMvcConfigurer {

    private final UserMdcInterceptor userMdcInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userMdcInterceptor)
                .addPathPatterns("/api/**");
    }
}