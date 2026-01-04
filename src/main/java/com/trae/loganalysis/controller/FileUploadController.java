package com.trae.loganalysis.controller;

import com.trae.loganalysis.entity.UploadFile;
import com.trae.loganalysis.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 上传文件
     * @param file 上传的文件
     * @param createdBy 创建者
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadFile> uploadFile(@RequestParam("file") MultipartFile file,
                                                @RequestParam(value = "createdBy", defaultValue = "anonymous") String createdBy) {
        logger.info("开始上传文件，文件名: {}, 创建者: {}", file.getOriginalFilename(), createdBy);
        try {
            UploadFile uploadFile = fileUploadService.uploadFile(file, createdBy);
            logger.info("文件上传成功，文件ID: {}", uploadFile.getId());
            return new ResponseEntity<>(uploadFile, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("文件上传失败，文件名: {}", file.getOriginalFilename(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取所有上传文件
     * @return 上传文件列表
     */
    @GetMapping
    public ResponseEntity<List<UploadFile>> getAllUploadFiles() {
        logger.info("获取所有上传文件");
        List<UploadFile> files = fileUploadService.getAllUploadFiles();
        logger.info("获取上传文件成功，共 {} 个文件", files.size());
        return new ResponseEntity<>(files, HttpStatus.OK);
    }

    /**
     * 分页获取上传文件
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果，包含总条数、页数等信息
     */
    @GetMapping("/page")
    public ResponseEntity<com.trae.loganalysis.model.PageResult<UploadFile>> getUploadFilesByPage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        logger.info("分页获取上传文件，页码: {}, 每页大小: {}", page, size);
        // 将前端从1开始的页码转换为后端从0开始的计算
        int offsetPage = page - 1;
        com.trae.loganalysis.model.PageResult<UploadFile> pageResult = fileUploadService.getUploadFilesByPage(offsetPage, size);
        logger.info("分页获取上传文件成功，总条数: {}, 总页数: {}", pageResult.getTotal(), pageResult.getTotalPages());
        return new ResponseEntity<>(pageResult, HttpStatus.OK);
    }

    /**
     * 根据ID获取上传文件
     * @param id 文件ID
     * @return 上传文件信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<UploadFile> getUploadFileById(@PathVariable Long id) {
        logger.info("根据ID获取上传文件，文件ID: {}", id);
        UploadFile file = fileUploadService.getUploadFileById(id);
        if (file == null) {
            logger.warn("未找到文件，文件ID: {}", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        logger.info("获取文件成功，文件ID: {}", id);
        return new ResponseEntity<>(file, HttpStatus.OK);
    }

}