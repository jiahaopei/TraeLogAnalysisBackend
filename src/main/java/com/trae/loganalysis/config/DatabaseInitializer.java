package com.trae.loganalysis.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    @Autowired
    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 检查是否存在upload_file表
            ResultSet uploadFileExists = metaData.getTables(null, null, "upload_file", new String[]{"TABLE"});
            boolean hasUploadFile = uploadFileExists.next();
            
            // 检查是否存在file_data表
            ResultSet fileDataExists = metaData.getTables(null, null, "file_data", new String[]{"TABLE"});
            boolean hasFileData = fileDataExists.next();
            
            // 检查是否存在analysis_result表
            ResultSet analysisResultExists = metaData.getTables(null, null, "analysis_result", new String[]{"TABLE"});
            boolean hasAnalysisResult = analysisResultExists.next();
            
            // 如果任何表不存在，执行schema.sql脚本
            if (!hasUploadFile || !hasFileData || !hasAnalysisResult) {
                System.out.println("Creating database tables...");
                ClassPathResource resource = new ClassPathResource("schema.sql");
                ScriptUtils.executeSqlScript(connection, resource);
                System.out.println("Database tables created successfully.");
            } else {
                System.out.println("All database tables already exist.");
            }
        }
    }
}