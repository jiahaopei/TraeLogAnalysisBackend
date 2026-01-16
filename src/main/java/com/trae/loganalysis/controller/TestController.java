package com.trae.loganalysis.controller;

import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.repository.FileDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private FileDataRepository fileDataRepository;

    /**
     * 获取所有file_data记录，用于测试
     * @return file_data记录列表
     */
    @GetMapping("/file-data")
    public List<FileData> getAllFileData() {
        return fileDataRepository.findAll();
    }
}