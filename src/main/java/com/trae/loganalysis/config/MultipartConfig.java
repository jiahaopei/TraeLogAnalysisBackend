package com.trae.loganalysis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MultipartConfig implements WebMvcConfigurer {
    // In Spring Boot 3.x, multipart configuration is handled via application properties
    // See: https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.multipart
    // Configuration moved to application-dev.yml
}
