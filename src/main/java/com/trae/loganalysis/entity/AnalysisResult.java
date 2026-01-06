package com.trae.loganalysis.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "analysis_result")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "file_data_id")
    private Long fileDataId;

    @Column(name = "result_content", columnDefinition = "TEXT", nullable = false)
    private String resultContent;

    @Column(name = "analysis_time", nullable = false)
    private Date analysisTime;

    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "log_info", columnDefinition = "TEXT")
    private String logInfo;
    
    @Column(name = "code")
    private String code;
    
    @Column(name = "class_name", columnDefinition = "TEXT")
    private String className;
    
    @Column(name = "line_number", columnDefinition = "INTEGER")
    private Integer lineNumber;
    
    @Column(name = "method_name", columnDefinition = "TEXT")
    private String methodName;
    
    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getFileDataId() {
        return fileDataId;
    }

    public void setFileDataId(Long fileDataId) {
        this.fileDataId = fileDataId;
    }

    public String getResultContent() {
        return resultContent;
    }

    public void setResultContent(String resultContent) {
        this.resultContent = resultContent;
    }

    public Date getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(Date analysisTime) {
        this.analysisTime = analysisTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLogInfo() {
        return logInfo;
    }

    public void setLogInfo(String logInfo) {
        this.logInfo = logInfo;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public Integer getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}