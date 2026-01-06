package com.trae.loganalysis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.trae.loganalysis.entity.AnalysisResult;
import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.repository.AnalysisResultRepository;
import com.trae.loganalysis.repository.FileDataRepository;
import com.trae.loganalysis.util.ExcelUtil;
import com.trae.loganalysis.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ResultExportService {

    private static final Logger logger = LoggerFactory.getLogger(ResultExportService.class);
    private final AnalysisResultRepository analysisResultRepository;
    private final FileDataRepository fileDataRepository;
    private final ExcelUtil excelUtil;
    private final FileUtil fileUtil;

    @Value("${file.upload.path}")
    private String uploadPath;

    public ResultExportService(AnalysisResultRepository analysisResultRepository,
                              FileDataRepository fileDataRepository,
                              ExcelUtil excelUtil,
                              FileUtil fileUtil) {
        this.analysisResultRepository = analysisResultRepository;
        this.fileDataRepository = fileDataRepository;
        this.excelUtil = excelUtil;
        this.fileUtil = fileUtil;
    }

    /**
     * 导出分析结果为Excel文件
     * @param fileId 文件ID
     * @return 导出文件路径
     */
    public String exportAnalysisResults(Long fileId) throws IOException {
        logger.info("开始导出分析结果，文件ID: {}", fileId);
        
        // 获取文件的所有分析结果
        List<AnalysisResult> results = analysisResultRepository.findByFileId(fileId);
        if (results.isEmpty()) {
            logger.warn("未找到文件的分析结果，文件ID: {}", fileId);
            throw new IllegalArgumentException("No analysis results found for file: " + fileId);
        }
        logger.info("找到 {} 条分析结果", results.size());

        // 获取所有相关的文件数据
        List<Long> fileDataIds = results.stream()
                .map(AnalysisResult::getFileDataId)
                .collect(Collectors.toList());
        List<FileData> fileDataList = fileDataRepository.findAllById(fileDataIds);

        // 创建数据映射
        Map<Long, FileData> fileDataMap = fileDataList.stream()
                .collect(Collectors.toMap(FileData::getId, fileData -> fileData));

        // 创建sheet数据（所有数据在一个sheet内）
        List<List<String>> sheetData = createSheetData(results, fileDataMap);

        // 创建sheet数据列表
        List<ExcelUtil.SheetData> sheetDataList = new ArrayList<>();
        sheetDataList.add(new ExcelUtil.SheetData("Analysis Results", sheetData));

        // 创建导出目录
        String exportPath = uploadPath + "exports/";
        fileUtil.createDirectory(exportPath);

        // 生成导出文件名
        String exportFilename = "analysis_result_" + fileId + "_" + UUID.randomUUID() + ".xlsx";
        String exportFilePath = exportPath + exportFilename;

        // 创建Excel文件
        excelUtil.createExcel(exportFilePath, sheetDataList);
        logger.info("分析结果导出成功，文件路径: {}", exportFilePath);

        return exportFilePath;
    }

    /**
     * 创建sheet数据（所有数据在一个sheet内，每条数据一行）
     * @param results 分析结果列表
     * @param fileDataMap 文件数据映射
     * @return sheet数据
     */
    private List<List<String>> createSheetData(List<AnalysisResult> results, Map<Long, FileData> fileDataMap) {
        List<List<String>> sheetData = new ArrayList<>();

        // 添加表头
        List<String> headerRow = new ArrayList<>();
        headerRow.add("column1");
        headerRow.add("column2");
        headerRow.add("column3");
        headerRow.add("column4");
        headerRow.add("错误日志");
        headerRow.add("相关源码");
        headerRow.add("AI初步分析");
        sheetData.add(headerRow);

        // 为每个分析结果创建一行数据
        for (AnalysisResult result : results) {
            FileData fileData = fileDataMap.get(result.getFileDataId());
            if (fileData == null) {
                logger.warn("未找到文件数据，fileDataId: {}", result.getFileDataId());
                continue;
            }

            List<String> dataRow = new ArrayList<>();
            
            // 添加fileData中的column1、column2、column3、column4
            dataRow.add(fileData.getColumn1() != null ? fileData.getColumn1() : "");
            dataRow.add(fileData.getColumn2() != null ? fileData.getColumn2() : "");
            dataRow.add(fileData.getColumn3() != null ? fileData.getColumn3() : "");
            dataRow.add(fileData.getColumn4() != null ? fileData.getColumn4() : "");
            
            // 添加analysisResult中的logInfo、code、resultContent
            dataRow.add(result.getLogInfo() != null ? result.getLogInfo() : "");
            dataRow.add(formatCodeForExcel(result.getCode()));
            dataRow.add(result.getResultContent() != null ? result.getResultContent() : "");
            
            sheetData.add(dataRow);
        }

        logger.info("创建sheet数据完成，共 {} 行数据（包含表头）", sheetData.size());
        return sheetData;
    }

    /**
     * 格式化code字段用于Excel导出，处理JSON数组的换行显示
     * @param code JSON字符串格式的源码列表
     * @return 格式化后的字符串，适合Excel显示
     */
    private String formatCodeForExcel(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        try {
            // 解析JSON数组
            JSONArray codeArray = JSON.parseArray(code);
            if (codeArray == null || codeArray.isEmpty()) {
                return "";
            }

            // 将每个源码元素用换行符连接
            StringBuilder formattedCode = new StringBuilder();
            for (int i = 0; i < codeArray.size(); i++) {
                String codeLine = codeArray.getString(i);
                if (codeLine != null) {
                    if (i > 0) {
                        formattedCode.append("\n");
                    }
                    formattedCode.append(codeLine);
                }
            }

            // Excel单元格有大小限制（32,767字符），需要截断过长的内容
            String result = formattedCode.toString();
            int maxLength = 32767; // Excel单元格最大字符数
            if (result.length() > maxLength) {
                result = result.substring(0, maxLength - 20) + "...(已截断，内容过长)";
                logger.warn("code字段内容过长，已截断。原始长度: {}, 截断后长度: {}", formattedCode.length(), result.length());
            }

            return result;
        } catch (Exception e) {
            logger.error("解析code字段JSON失败: {}", code, e);
            // 如果解析失败，直接返回原始字符串，但也需要检查长度
            if (code.length() > 32767) {
                logger.warn("code字段内容过长，已截断。原始长度: {}", code.length());
                return code.substring(0, 32747) + "...(已截断，内容过长)";
            }
            return code;
        }
    }

    /**
     * 获取导出文件的File对象
     * @param filePath 文件路径
     * @return 文件对象
     */
    public File getExportFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Export file not found: " + filePath);
        }
        return file;
    }
}