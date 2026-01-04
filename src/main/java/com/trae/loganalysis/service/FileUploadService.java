package com.trae.loganalysis.service;

import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.entity.UploadFile;
import com.trae.loganalysis.repository.FileDataRepository;
import com.trae.loganalysis.repository.UploadFileRepository;
import com.trae.loganalysis.util.ExcelUtil;
import com.trae.loganalysis.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
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
        logger.info("处理文件上传，原始文件名: {}, 创建者: {}", file.getOriginalFilename(), createdBy);
        
        // 检查文件是否为Excel文件
        if (!fileUtil.isExcelFile(file.getOriginalFilename())) {
            logger.warn("文件格式不支持，文件名: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("Only Excel files are supported");
        }

        // 创建上传目录
        fileUtil.createDirectory(uploadPath);
        logger.debug("上传目录创建成功: {}", uploadPath);

        // 生成唯一文件名
        String uniqueFilename = fileUtil.generateUniqueFilename(file.getOriginalFilename());
        String filePath = uploadPath + uniqueFilename;
        logger.debug("生成唯一文件名: {}, 保存路径: {}", uniqueFilename, filePath);

        // 保存文件到本地
        File dest = new File(filePath);
        file.transferTo(dest);
        logger.debug("文件保存到本地成功: {}", filePath);

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
        logger.info("文件记录保存到数据库成功，文件ID: {}", savedFile.getId());

        // 异步读取Excel文件内容并保存到数据库
        CompletableFuture.runAsync(() -> {
            logger.info("开始异步读取Excel文件内容，文件ID: {}", savedFile.getId());
            try {
                readAndSaveExcelData(savedFile.getId(), filePath);
                logger.info("Excel文件内容读取并保存成功，文件ID: {}", savedFile.getId());
            } catch (IOException e) {
                // 更新状态为失败
                logger.error("读取Excel文件失败，文件ID: {}", savedFile.getId(), e);
                savedFile.setStatus("FAILED");
                savedFile.setErrorMessage("Failed to read Excel file: " + e.getMessage());
                uploadFileRepository.save(savedFile);
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
        logger.info("读取Excel文件成功，文件ID: {}, 共 {} 行数据", fileId, excelData.size());

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
        logger.info("Excel数据保存到数据库成功，文件ID: {}, 共 {} 行数据", fileId, excelData.size());
    }

    /**
     * 根据ID获取上传文件信息
     * @param id 文件ID
     * @return 上传文件信息
     */
    public UploadFile getUploadFileById(Long id) {
        logger.info("根据ID查询文件，文件ID: {}", id);
        UploadFile file = uploadFileRepository.findById(id).orElse(null);
        if (file != null) {
            logger.debug("查询到文件，文件ID: {}, 文件名: {}", id, file.getFileName());
        } else {
            logger.debug("未查询到文件，文件ID: {}", id);
        }
        return file;
    }

    /**
     * 获取所有上传文件
     * @return 上传文件列表
     */
    public List<UploadFile> getAllUploadFiles() {
        logger.info("获取所有上传文件");
        List<UploadFile> files = uploadFileRepository.findAll();
        logger.info("获取所有上传文件成功，共 {} 个文件", files.size());
        return files;
    }

    /**
     * 分页获取上传文件
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果，包含总条数、页数等信息
     */
    public com.trae.loganalysis.model.PageResult<UploadFile> getUploadFilesByPage(int page, int size) {
        logger.info("分页查询文件，页码: {}, 每页大小: {}", page, size);
        // 使用SQLite兼容的LIMIT OFFSET语法，避免使用Pageable生成不兼容的SQL
        int offset = page * size;
        logger.debug("计算分页偏移量: offset = page * size = {} * {} = {}", page, size, offset);
        
        List<UploadFile> files = uploadFileRepository.findAllByOrderByIdAsc(offset, size);
        long total = uploadFileRepository.count();
        
        logger.info("分页查询成功，总条数: {}, 当前页数据条数: {}", total, files.size());
        return new com.trae.loganalysis.model.PageResult<>(page, size, total, files);
    }
}