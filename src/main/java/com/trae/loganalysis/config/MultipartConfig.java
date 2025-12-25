package com.trae.loganalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
public class MultipartConfig {

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(104857600); // 100MB
        resolver.setMaxUploadSizePerFile(10485760); // 10MB
        resolver.setDefaultEncoding("UTF-8");
        return resolver;
    }

}