package com.trae.loganalysis.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

    /**
     * 读取Excel文件内容
     * @param filePath 文件路径
     * @return 每行数据的列表
     */
    public List<List<String>> readExcel(String filePath) throws IOException {
        logger.info("开始读取Excel文件: {}", filePath);
        List<List<String>> result = new ArrayList<>();
        File file = new File(filePath);
        Workbook workbook = null;

        try (FileInputStream fis = new FileInputStream(file)) {
            // 根据文件扩展名创建对应的Workbook
            if (filePath.endsWith(".xlsx")) {
                logger.debug("创建XSSFWorkbook处理.xlsx文件: {}", filePath);
                workbook = new XSSFWorkbook(fis);
            } else if (filePath.endsWith(".xls")) {
                logger.debug("创建HSSFWorkbook处理.xls文件: {}", filePath);
                workbook = new HSSFWorkbook(fis);
            } else {
                logger.error("不支持的文件格式: {}", filePath);
                throw new IllegalArgumentException("Unsupported file format: " + filePath);
            }

            // 读取第一个sheet
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                logger.warn("Excel文件中没有找到sheet: {}", filePath);
                return result;
            }

            logger.debug("开始读取sheet: {}, 总行数: {}", sheet.getSheetName(), sheet.getLastRowNum() + 1);
            // 遍历所有行
            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                // 获取最大列数
                int maxColumn = row.getLastCellNum();
                for (int i = 0; i < maxColumn; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellType(CellType.STRING);
                    rowData.add(cell.getStringCellValue());
                }
                result.add(rowData);
            }
            logger.info("读取Excel文件成功: {}, 共读取 {} 行数据", filePath, result.size());
        } catch (IOException e) {
            logger.error("读取Excel文件失败: {}", filePath, e);
            throw e;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                    logger.debug("成功关闭Workbook资源");
                } catch (IOException e) {
                    logger.error("关闭Workbook资源失败", e);
                }
            }
        }

        return result;
    }

    /**
     * 创建Excel文件
     * @param filePath 输出文件路径
     * @param sheetNameMap sheet名称与数据的映射
     */
    public void createExcel(String filePath, List<SheetData> sheetDataList) throws IOException {
        logger.info("开始创建Excel文件: {}", filePath);
        Workbook workbook = null;

        // 根据文件扩展名创建对应的Workbook
        if (filePath.endsWith(".xlsx")) {
            logger.debug("创建XSSFWorkbook用于生成.xlsx文件: {}", filePath);
            workbook = new XSSFWorkbook();
        } else if (filePath.endsWith(".xls")) {
            logger.debug("创建HSSFWorkbook用于生成.xls文件: {}", filePath);
            workbook = new HSSFWorkbook();
        } else {
            logger.error("不支持的文件格式: {}", filePath);
            throw new IllegalArgumentException("Unsupported file format: " + filePath);
        }

        // 为每个sheet创建数据
        for (SheetData sheetData : sheetDataList) {
            Sheet sheet = workbook.createSheet(sheetData.getSheetName());
            List<List<String>> data = sheetData.getData();
            logger.debug("创建sheet: {}, 包含 {} 行数据", sheetData.getSheetName(), data.size());

            // 创建行和单元格
            for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                List<String> rowData = data.get(rowIndex);

                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    cell.setCellValue(rowData.get(colIndex));
                }
            }
        }

        // 保存文件
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
            logger.info("Excel文件创建成功: {}", filePath);
        } catch (IOException e) {
            logger.error("创建Excel文件失败: {}", filePath, e);
            throw e;
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                    logger.debug("成功关闭Workbook资源");
                } catch (IOException e) {
                    logger.error("关闭Workbook资源失败", e);
                }
            }
        }
    }

    /**
     * Sheet数据封装类
     */
    public static class SheetData {
        private String sheetName;
        private List<List<String>> data;

        public SheetData(String sheetName, List<List<String>> data) {
            this.sheetName = sheetName;
            this.data = data;
        }

        public String getSheetName() {
            return sheetName;
        }

        public List<List<String>> getData() {
            return data;
        }
    }
}