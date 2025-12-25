package com.trae.loganalysis.controller;

import com.trae.loganalysis.service.ResultExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
            // 导出结果到文件
            String exportFilePath = resultExportService.exportAnalysisResults(fileId);
            File file = new File(exportFilePath);
            Resource resource = new FileSystemResource(file);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "analysis_result_" + fileId + ".xlsx");
            headers.setContentLength(file.length());

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}