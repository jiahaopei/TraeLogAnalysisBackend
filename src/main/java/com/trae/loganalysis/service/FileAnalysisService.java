package com.trae.loganalysis.service;

import com.trae.loganalysis.entity.AnalysisResult;
import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.entity.UploadFile;
import com.trae.loganalysis.repository.AnalysisResultRepository;
import com.trae.loganalysis.repository.FileDataRepository;
import com.trae.loganalysis.repository.UploadFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class FileAnalysisService {

    private final UploadFileRepository uploadFileRepository;
    private final FileDataRepository fileDataRepository;
    private final AnalysisResultRepository analysisResultRepository;

    private final ExecutorService executorService;

    public FileAnalysisService(UploadFileRepository uploadFileRepository,
                              FileDataRepository fileDataRepository,
                              AnalysisResultRepository analysisResultRepository,
                              @Value("${file.analysis.thread-pool-size}") int threadPoolSize) {
        this.uploadFileRepository = uploadFileRepository;
        this.fileDataRepository = fileDataRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * 分析文件
     * @param fileId 文件ID
     */
    public void analyzeFile(Long fileId) {
        // 获取文件信息
        UploadFile uploadFile = uploadFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        // 检查文件状态
        if (!"UPLOADED".equals(uploadFile.getStatus())) {
            throw new IllegalStateException("File is not in UPLOADED status: " + uploadFile.getStatus());
        }

        // 更新状态为分析中
        uploadFile.setStatus("ANALYZING");
        uploadFileRepository.save(uploadFile);

        // 异步执行分析
        CompletableFuture.runAsync(() -> {
            try {
                // 获取文件数据
                List<FileData> fileDataList = fileDataRepository.findByFileId(fileId);

                // 多线程分析每个数据行
                List<CompletableFuture<Void>> futures = fileDataList.stream()
                        .map(fileData -> CompletableFuture.runAsync(
                                () -> analyzeDataRow(fileId, fileData), executorService))
                        .toList();

                // 等待所有分析完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // 更新状态为分析完成
                uploadFile.setStatus("COMPLETED");
            } catch (Exception e) {
                // 更新状态为失败
                uploadFile.setStatus("FAILED");
                uploadFile.setErrorMessage("Analysis failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // 保存状态更新
                uploadFileRepository.save(uploadFile);
            }
        }, executorService);
    }

    /**
     * 分析数据行
     * @param fileId 文件ID
     * @param fileData 数据行
     */
    private void analyzeDataRow(Long fileId, FileData fileData) {
        try {
            // 模拟数据分析过程
            Thread.sleep(1000); // 模拟耗时操作

            // 创建分析结果
            AnalysisResult result = new AnalysisResult();
            result.setFileId(fileId);
            result.setFileDataId(fileData.getId());
            result.setAnalysisTime(new Date());
            result.setStatus("SUCCESS");

            // 简单的分析逻辑：统计数据长度和内容
            StringBuilder resultContent = new StringBuilder();
            resultContent.append("{\n")
                    .append("  \"rowIndex\": ").append(fileData.getRowIndex()).append(",\n")
                    .append("  \"column1\": \"").append(fileData.getColumn1()).append("\",\n")
                    .append("  \"column2\": \"").append(fileData.getColumn2()).append("\",\n")
                    .append("  \"column3\": \"").append(fileData.getColumn3()).append("\",\n")
                    .append("  \"dataContent\": ").append(fileData.getDataContent()).append(",\n")
                    .append("  \"analysis\": {").append("\n")
                    .append("    \"column1Length\": ").append(fileData.getColumn1() != null ? fileData.getColumn1().length() : 0).append(",\n")
                    .append("    \"column2Length\": ").append(fileData.getColumn2() != null ? fileData.getColumn2().length() : 0).append(",\n")
                    .append("    \"column3Length\": ").append(fileData.getColumn3() != null ? fileData.getColumn3().length() : 0).append(",\n")
                    .append("    \"hasDataContent\": ").append(fileData.getDataContent() != null && !fileData.getDataContent().isEmpty()).append("\n")
                    .append("  }\n")
                    .append("}");

            result.setResultContent(resultContent.toString());

            // 保存分析结果
            analysisResultRepository.save(result);
        } catch (Exception e) {
            // 记录失败的分析结果
            AnalysisResult result = new AnalysisResult();
            result.setFileId(fileId);
            result.setFileDataId(fileData.getId());
            result.setAnalysisTime(new Date());
            result.setStatus("FAILED");
            result.setResultContent("Analysis failed: " + e.getMessage());
            analysisResultRepository.save(result);
        }
    }

    /**
     * 获取文件的分析结果
     * @param fileId 文件ID
     * @return 分析结果列表
     */
    public List<AnalysisResult> getAnalysisResults(Long fileId) {
        return analysisResultRepository.findByFileId(fileId);
    }

    /**
     * 获取数据行的分析结果
     * @param fileDataId 数据行ID
     * @return 分析结果列表
     */
    public List<AnalysisResult> getAnalysisResultsByDataId(Long fileDataId) {
        return analysisResultRepository.findByFileDataId(fileDataId);
    }
}