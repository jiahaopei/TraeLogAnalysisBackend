package com.trae.loganalysis.controller;

import com.trae.loganalysis.entity.AnalysisResult;
import com.trae.loganalysis.service.FileAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final FileAnalysisService fileAnalysisService;

    public AnalysisController(FileAnalysisService fileAnalysisService) {
        this.fileAnalysisService = fileAnalysisService;
    }

    /**
     * 开始分析文件
     * @param fileId 文件ID
     * @return 分析结果
     */
    @PostMapping("/start/{fileId}")
    public ResponseEntity<String> startAnalysis(@PathVariable Long fileId) {
        try {
            fileAnalysisService.analyzeFile(fileId);
            return new ResponseEntity<>("Analysis started successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取文件分析结果
     * @param fileId 文件ID
     * @return 分析结果列表
     */
    @GetMapping("/results/{fileId}")
    public ResponseEntity<List<AnalysisResult>> getAnalysisResults(@PathVariable Long fileId) {
        try {
            List<AnalysisResult> results = fileAnalysisService.getAnalysisResults(fileId);
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取数据行分析结果
     * @param fileDataId 数据行ID
     * @return 分析结果列表
     */
    @GetMapping("/results/data/{fileDataId}")
    public ResponseEntity<List<AnalysisResult>> getAnalysisResultsByDataId(@PathVariable Long fileDataId) {
        try {
            List<AnalysisResult> results = fileAnalysisService.getAnalysisResultsByDataId(fileDataId);
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}