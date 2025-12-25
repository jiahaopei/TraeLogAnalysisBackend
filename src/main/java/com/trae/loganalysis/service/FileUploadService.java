package com.trae.loganalysis.service;

import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.entity.UploadFile;
import com.trae.loganalysis.repository.FileDataRepository;
import com.trae.loganalysis.repository.UploadFileRepository;
import com.trae.loganalysis.util.ExcelUtil;
import com.trae.loganalysis.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FileUploadService {

    private final UploadFileRepository uploadFileRepository;
    private final FileDataRepository fileDataRepository;
    private final FileUtil fileUtil;
    private final ExcelUtil excelUtil;

    @Value("${file.upload.path}")
    private String uploadPath;

    public FileUploadService(UploadFileRepository uploadFileRepository,
                             FileDataRepository fileDataRepository,
                             FileUtil fileUtil,
                             ExcelUtil excelUtil) {
        this.uploadFileRepository = uploadFileRepository;
        this.fileDataRepository = fileDataRepository;
        this.fileUtil = fileUtil;
        this.excelUtil = excelUtil;
    }

    /**
     * 上传文件
     * @param file 上传的文件
     * @param createdBy 创建者
     * @return 上传文件信息
     */
    public UploadFile uploadFile(MultipartFile file, String createdBy) throws IOException {
        // 检查文件是否为Excel文件
        if (!fileUtil.isExcelFile(file.getOriginalFilename())) {
            throw new IllegalArgumentException("Only Excel files are supported");
        }

        // 创建上传目录
        fileUtil.createDirectory(uploadPath);

        // 生成唯一文件名
        String uniqueFilename = fileUtil.generateUniqueFilename(file.getOriginalFilename());
        String filePath = uploadPath + uniqueFilename;

        // 保存文件到本地
        File dest = new File(filePath);
        file.transferTo(dest);

        // 创建上传文件记录
        UploadFile uploadFile = new UploadFile();
        uploadFile.setFileName(file.getOriginalFilename());
        uploadFile.setFilePath(filePath);
        uploadFile.setFileSize(file.getSize());
        uploadFile.setUploadTime(new Date());
        uploadFile.setStatus("UPLOADED"); // 初始状态为已上传
        uploadFile.setCreatedBy(createdBy);

        // 保存到数据库
        UploadFile savedFile = uploadFileRepository.save(uploadFile);

        // 异步读取Excel文件内容并保存到数据库
        CompletableFuture.runAsync(() -> {
            try {
                readAndSaveExcelData(savedFile.getId(), filePath);
            } catch (IOException e) {
                // 更新状态为失败
                savedFile.setStatus("FAILED");
                savedFile.setErrorMessage("Failed to read Excel file: " + e.getMessage());
                uploadFileRepository.save(savedFile);
                e.printStackTrace();
            }
        });

        return savedFile;
    }

    /**
     * 读取Excel文件内容并保存到数据库
     * @param fileId 文件ID
     * @param filePath 文件路径
     */
    private void readAndSaveExcelData(Long fileId, String filePath) throws IOException {
        // 读取Excel文件
        List<List<String>> excelData = excelUtil.readExcel(filePath);

        // 遍历数据并保存到数据库
        for (int i = 0; i < excelData.size(); i++) {
            List<String> rowData = excelData.get(i);
            FileData fileData = new FileData();
            fileData.setFileId(fileId);
            fileData.setRowIndex(i);

            // 保存前四列数据
            if (rowData.size() >= 1) {
                fileData.setColumn1(rowData.get(0));
            }
            if (rowData.size() >= 2) {
                fileData.setColumn2(rowData.get(1));
            }
            if (rowData.size() >= 3) {
                fileData.setColumn3(rowData.get(2));
            }
            if (rowData.size() >= 4) {
                fileData.setColumn4(rowData.get(3));
            }

            // 保存其他列数据（JSON格式）
            if (rowData.size() > 4) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("[");
                for (int j = 4; j < rowData.size(); j++) {
                    jsonBuilder.append('"').append(rowData.get(j)).append('"');
                    if (j < rowData.size() - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");
                fileData.setDataContent(jsonBuilder.toString());
            }

            // 保存到数据库
            fileDataRepository.save(fileData);
        }
    }

    /**
     * 根据ID获取上传文件信息
     * @param id 文件ID
     * @return 上传文件信息
     */
    public UploadFile getUploadFileById(Long id) {
        return uploadFileRepository.findById(id).orElse(null);
    }

    /**
     * 获取所有上传文件
     * @return 上传文件列表
     */
    public List<UploadFile> getAllUploadFiles() {
        return uploadFileRepository.findAll();
    }

    /**
     * 分页获取上传文件
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    public org.springframework.data.domain.Page<UploadFile> getUploadFilesByPage(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<UploadFile> all = uploadFileRepository.findAll(pageable);
        return all;
    }
}