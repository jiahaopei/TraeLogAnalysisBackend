package com.trae.loganalysis.config;

import org.springframework.beans.factory.annotation.Value;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Bean
    @Primary
    public DataSource dataSource() {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(false);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        
        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl(url);
        
        return dataSource;
    }
}
