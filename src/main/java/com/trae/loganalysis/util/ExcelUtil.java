package com.trae.loganalysis.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelUtil {

    /**
     * 读取Excel文件内容
     * @param filePath 文件路径
     * @return 每行数据的列表
     */
    public List<List<String>> readExcel(String filePath) throws IOException {
        List<List<String>> result = new ArrayList<>();
        File file = new File(filePath);
        Workbook workbook = null;

        try (FileInputStream fis = new FileInputStream(file)) {
            // 根据文件扩展名创建对应的Workbook
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (filePath.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + filePath);
            }

            // 读取第一个sheet
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return result;
            }

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
        } finally {
            if (workbook != null) {
                workbook.close();
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
        Workbook workbook = null;

        // 根据文件扩展名创建对应的Workbook
        if (filePath.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook();
        } else if (filePath.endsWith(".xls")) {
            workbook = new HSSFWorkbook();
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + filePath);
        }

        // 为每个sheet创建数据
        for (SheetData sheetData : sheetDataList) {
            Sheet sheet = workbook.createSheet(sheetData.getSheetName());
            List<List<String>> data = sheetData.getData();

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
        } finally {
            if (workbook != null) {
                workbook.close();
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