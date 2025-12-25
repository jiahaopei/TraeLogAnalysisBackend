package com.trae.loganalysis.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Component
public class FileUtil {

    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public boolean createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    /**
     * 生成唯一文件名
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public String generateUniqueFilename(String originalFilename) {
        // 获取文件扩展名
        String extension = getFileExtension(originalFilename);
        // 生成UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 生成时间戳
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        // 组合成新的文件名
        return timestamp + "_" + uuid + "." + extension;
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
        String extension = getFileExtension(filename).toLowerCase();
        return extension.equals("xlsx") || extension.equals("xls");
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }
}