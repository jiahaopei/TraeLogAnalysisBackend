package com.trae.loganalysis.controller;

import com.trae.loganalysis.service.ResultExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private static final Logger logger = LoggerFactory.getLogger(ResultController.class);
    private final ResultExportService resultExportService;

    public ResultController(ResultExportService resultExportService) {
        this.resultExportService = resultExportService;
    }

    /**
     * 导出分析结果
     * @param fileId 文件ID
     * @return 导出的Excel文件
     */
    @GetMapping("/export/{fileId}")
    public ResponseEntity<Resource> exportResults(@PathVariable Long fileId) {
        try {
            logger.info("开始导出分析结果，文件ID: {}", fileId);
            
            // 导出结果到文件
            String exportFilePath = resultExportService.exportAnalysisResults(fileId);
            File file = new File(exportFilePath);
            
            if (!file.exists()) {
                logger.error("导出文件不存在: {}", exportFilePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "analysis_result_" + fileId + ".xlsx");
            headers.setContentLength(file.length());

            logger.info("分析结果导出成功，文件路径: {}", exportFilePath);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            logger.error("导出分析结果失败，参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("导出分析结果失败，IO异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("导出分析结果失败，未知异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}