package com.trae.loganalysis.service;

import com.trae.loganalysis.entity.AnalysisResult;
import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.repository.AnalysisResultRepository;
import com.trae.loganalysis.repository.FileDataRepository;
import com.trae.loganalysis.util.ExcelUtil;
import com.trae.loganalysis.util.FileUtil;
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
        // 获取文件的所有分析结果
        List<AnalysisResult> results = analysisResultRepository.findByFileId(fileId);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("No analysis results found for file: " + fileId);
        }

        // 获取所有相关的文件数据
        List<Long> fileDataIds = results.stream()
                .map(AnalysisResult::getFileDataId)
                .collect(Collectors.toList());
        List<FileData> fileDataList = fileDataRepository.findAllById(fileDataIds);

        // 创建数据映射
        Map<Long, FileData> fileDataMap = fileDataList.stream()
                .collect(Collectors.toMap(FileData::getId, fileData -> fileData));

        // 创建sheet数据列表
        List<ExcelUtil.SheetData> sheetDataList = new ArrayList<>();

        // 为每个分析结果创建一个sheet
        for (AnalysisResult result : results) {
            FileData fileData = fileDataMap.get(result.getFileDataId());
            if (fileData == null) {
                continue;
            }

            // 创建sheet名称（使用前三列组合）
            String sheetName = createSheetName(fileData);

            // 创建sheet数据
            List<List<String>> sheetData = createSheetData(result, fileData);

            // 添加到sheet数据列表
            sheetDataList.add(new ExcelUtil.SheetData(sheetName, sheetData));
        }

        // 创建导出目录
        String exportPath = uploadPath + "exports/";
        fileUtil.createDirectory(exportPath);

        // 生成导出文件名
        String exportFilename = "analysis_result_" + fileId + "_" + UUID.randomUUID() + ".xlsx";
        String exportFilePath = exportPath + exportFilename;

        // 创建Excel文件
        excelUtil.createExcel(exportFilePath, sheetDataList);

        return exportFilePath;
    }

    /**
     * 创建sheet名称
     * @param fileData 文件数据
     * @return sheet名称
     */
    private String createSheetName(FileData fileData) {
        // 使用前三列组合，最多31个字符（Excel sheet名称限制）
        StringBuilder sheetName = new StringBuilder();
        if (fileData.getColumn1() != null) {
            sheetName.append(fileData.getColumn1()).append("_");
        }
        if (fileData.getColumn2() != null) {
            sheetName.append(fileData.getColumn2()).append("_");
        }
        if (fileData.getColumn3() != null) {
            sheetName.append(fileData.getColumn3());
        }

        // 移除末尾的下划线
        if (sheetName.length() > 0 && sheetName.charAt(sheetName.length() - 1) == '_') {
            sheetName.deleteCharAt(sheetName.length() - 1);
        }

        // 限制sheet名称长度为31个字符
        if (sheetName.length() > 31) {
            sheetName = new StringBuilder(sheetName.substring(0, 31));
        }

        // 如果sheet名称为空，使用默认名称
        if (sheetName.length() == 0) {
            sheetName.append("Sheet_");
        }

        return sheetName.toString();
    }

    /**
     * 创建sheet数据
     * @param result 分析结果
     * @param fileData 文件数据
     * @return sheet数据
     */
    private List<List<String>> createSheetData(AnalysisResult result, FileData fileData) {
        List<List<String>> sheetData = new ArrayList<>();

        // 添加标题行
        List<String> titleRow = new ArrayList<>();
        titleRow.add("Field");
        titleRow.add("Value");
        sheetData.add(titleRow);

        // 添加数据行
        List<String> row1 = new ArrayList<>();
        row1.add("Row Index");
        row1.add(String.valueOf(fileData.getRowIndex()));
        sheetData.add(row1);

        List<String> row2 = new ArrayList<>();
        row2.add("Column 1");
        row2.add(fileData.getColumn1());
        sheetData.add(row2);

        List<String> row3 = new ArrayList<>();
        row3.add("Column 2");
        row3.add(fileData.getColumn2());
        sheetData.add(row3);

        List<String> row4 = new ArrayList<>();
        row4.add("Column 3");
        row4.add(fileData.getColumn3());
        sheetData.add(row4);

        List<String> row5 = new ArrayList<>();
        row5.add("Data Content");
        row5.add(fileData.getDataContent());
        sheetData.add(row5);

        List<String> row6 = new ArrayList<>();
        row6.add("Analysis Status");
        row6.add(result.getStatus());
        sheetData.add(row6);

        List<String> row7 = new ArrayList<>();
        row7.add("Analysis Time");
        row7.add(result.getAnalysisTime().toString());
        sheetData.add(row7);

        List<String> row8 = new ArrayList<>();
        row8.add("Analysis Result");
        row8.add(result.getResultContent());
        sheetData.add(row8);

        return sheetData;
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