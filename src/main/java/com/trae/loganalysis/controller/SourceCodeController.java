package com.trae.loganalysis.controller;

import com.trae.loganalysis.service.SourceCodeParserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sourcecode")
public class SourceCodeController {

    private final SourceCodeParserService sourceCodeParserService;

    public SourceCodeController(SourceCodeParserService sourceCodeParserService) {
        this.sourceCodeParserService = sourceCodeParserService;
    }

    /**
     * 重新扫描并加载源代码到缓存
     * @return 扫描结果信息
     */
    @GetMapping("/reload")
    public ResponseEntity<String> reloadSourceCode() {
        try {
            String result = sourceCodeParserService.reloadSourceCode();
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("重新扫描失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取缓存状态
     * @return 缓存状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<String> getCacheStatus() {
        try {
            String status = sourceCodeParserService.getCacheStatus();
            return new ResponseEntity<>(status, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("获取缓存状态失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
