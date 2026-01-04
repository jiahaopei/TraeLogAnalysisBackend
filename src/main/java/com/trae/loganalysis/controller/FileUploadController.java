package com.trae.loganalysis.controller;

import com.trae.loganalysis.entity.UploadFile;
import com.trae.loganalysis.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

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
        try {
            UploadFile uploadFile = fileUploadService.uploadFile(file, createdBy);
            return new ResponseEntity<>(uploadFile, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取所有上传文件
     * @return 上传文件列表
     */
    @GetMapping
    public ResponseEntity<List<UploadFile>> getAllUploadFiles() {
        List<UploadFile> files = fileUploadService.getAllUploadFiles();
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
        // 将前端从1开始的页码转换为后端从0开始的计算
        int offsetPage = page - 1;
        com.trae.loganalysis.model.PageResult<UploadFile> pageResult = fileUploadService.getUploadFilesByPage(offsetPage, size);
        return new ResponseEntity<>(pageResult, HttpStatus.OK);
    }

    /**
     * 根据ID获取上传文件
     * @param id 文件ID
     * @return 上传文件信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<UploadFile> getUploadFileById(@PathVariable Long id) {
        UploadFile file = fileUploadService.getUploadFileById(id);
        if (file == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(file, HttpStatus.OK);
    }

}