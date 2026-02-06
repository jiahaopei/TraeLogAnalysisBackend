package com.trae.loganalysis.controller;

import com.trae.loganalysis.service.SourceCodeParserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 上传代码文件（支持zip或tar.gz格式）
     * @param file 上传的文件
     * @return 解压结果信息
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadSourceCode(@RequestParam("file") MultipartFile file) {
        try {
            SourceCodeParserService.ExtractResult result = sourceCodeParserService.uploadAndExtractFile(file);
            
            String message = "解压路径: " + result.getExtractPath() + "。" + result.getSummary();
            
            if (result.isAllFailed()) {
                return new ResponseEntity<>("文件解压失败，所有文件都无法解压。" + message, 
                                         HttpStatus.INTERNAL_SERVER_ERROR);
            } else if (result.hasFailures()) {
                return new ResponseEntity<>("文件部分解压成功。" + message + 
                                         "。失败文件: " + String.join(", ", result.getFailedFiles()), 
                                         HttpStatus.OK);
            } else {
                return new ResponseEntity<>("文件上传并解压成功。" + message, HttpStatus.OK);
            }
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("参数错误: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("文件上传失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
