package com.trae.loganalysis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Component
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public boolean createDirectory(String dirPath) {
        logger.debug("开始创建目录: {}", dirPath);
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (result) {
                logger.info("目录创建成功: {}", dirPath);
            } else {
                logger.error("目录创建失败: {}", dirPath);
            }
            return result;
        }
        logger.debug("目录已存在: {}", dirPath);
        return true;
    }

    /**
     * 生成唯一文件名
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public String generateUniqueFilename(String originalFilename) {
        logger.debug("为原始文件名生成唯一文件名: {}", originalFilename);
        // 获取文件扩展名
        String extension = getFileExtension(originalFilename);
        // 生成UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 生成时间戳
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        // 组合成新的文件名
        String uniqueFilename = timestamp + "_" + uuid + "." + extension;
        logger.debug("生成唯一文件名成功: {} -> {}", originalFilename, uniqueFilename);
        return uniqueFilename;
    }

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 文件扩展名
     */
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 检查文件是否为Excel文件
     * @param filename 文件名
     * @return 是否为Excel文件
     */
    public boolean isExcelFile(String filename) {
        logger.debug("检查文件是否为Excel文件: {}", filename);
        String extension = getFileExtension(filename).toLowerCase();
        boolean isExcel = extension.equals("xlsx") || extension.equals("xls");
        logger.debug("文件 {} 是Excel文件: {}", filename, isExcel);
        return isExcel;
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        logger.debug("开始删除文件: {}", filePath);
        File file = new File(filePath);
        if (file.exists()) {
            boolean result = file.delete();
            if (result) {
                logger.info("文件删除成功: {}", filePath);
            } else {
                logger.error("文件删除失败: {}", filePath);
            }
            return result;
        }
        logger.debug("文件不存在，无需删除: {}", filePath);
        return true;
    }
}