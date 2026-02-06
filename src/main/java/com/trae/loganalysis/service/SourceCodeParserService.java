package com.trae.loganalysis.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SourceCodeParserService implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(SourceCodeParserService.class);
    
    public static class ExtractResult {
        private int successCount;
        private int failureCount;
        private List<String> failedFiles;
        private String extractPath;
        
        public ExtractResult() {
            this.successCount = 0;
            this.failureCount = 0;
            this.failedFiles = new ArrayList<>();
        }
        
        public void addSuccess() {
            successCount++;
        }
        
        public void addFailure(String fileName) {
            failureCount++;
            failedFiles.add(fileName);
        }
        
        public void setExtractPath(String extractPath) {
            this.extractPath = extractPath;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        public List<String> getFailedFiles() {
            return failedFiles;
        }
        
        public String getExtractPath() {
            return extractPath;
        }
        
        public boolean isAllFailed() {
            return successCount == 0 && failureCount > 0;
        }
        
        public boolean hasFailures() {
            return failureCount > 0;
        }
        
        public String getSummary() {
            if (failureCount == 0) {
                return String.format("全部成功，共解压 %d 个文件", successCount);
            } else if (successCount == 0) {
                return String.format("全部失败，共 %d 个文件解压失败", failureCount);
            } else {
                return String.format("部分成功，成功 %d 个文件，失败 %d 个文件", successCount, failureCount);
            }
        }
    }
    
    @Autowired
    private SourceCodeCache sourceCodeCache;
    
    @Value("${source-code.directories}")
    private String directoriesConfig;
    
    @Value("${upload.directory}")
    private String uploadDirectory;
    
    @Override
    public void run(ApplicationArguments args) {
        reloadSourceCode();
    }
    
    /**
     * 重新扫描并加载源代码到缓存
     * @return 扫描结果信息
     */
    public String reloadSourceCode() {
        logger.info("开始重新解析Java源代码文件...");
        
        // 清空现有缓存
        sourceCodeCache.clear();
        
        String[] directories = directoriesConfig.split(",");
        int totalFiles = 0;
        int successFiles = 0;
        
        for (String dir : directories) {
            dir = dir.trim();
            if (dir.isEmpty()) {
                continue;
            }
            
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                logger.warn("目录不存在，跳过: {}", dir);
                continue;
            }
            
            logger.info("扫描目录: {}", dir);
            
            List<File> javaFiles = findJavaFiles(path.toFile());
            totalFiles += javaFiles.size();
            
            for (File javaFile : javaFiles) {
                try {
                    parseJavaFile(javaFile);
                    successFiles++;
                } catch (Exception e) {
                    logger.error("解析文件失败: {}", javaFile.getPath(), e);
                }
            }
        }
        
        String result = String.format("Java源代码重新解析完成。总文件数: %d, 成功解析: %d, 缓存类数: %d", 
                                      totalFiles, successFiles, sourceCodeCache.size());
        logger.info(result);
        return result;
    }
    
    /**
     * 获取缓存状态
     * @return 缓存状态信息
     */
    public String getCacheStatus() {
        return String.format("当前缓存状态：已缓存 %d 个类的源代码", sourceCodeCache.size());
    }
    
    private List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        findJavaFilesRecursive(directory, javaFiles);
        return javaFiles;
    }
    
    private void findJavaFilesRecursive(File directory, List<File> javaFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                findJavaFilesRecursive(file, javaFiles);
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }
    
    private void parseJavaFile(File javaFile) throws IOException {
        String sourceCode = Files.readString(javaFile.toPath());
        
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(sourceCode).getResult().orElse(null);
        
        if (cu == null) {
            logger.warn("解析失败，无法创建CompilationUnit: {}", javaFile.getPath());
            throw new IOException("无法创建CompilationUnit");
        }
        
        String className = null;
        
        // 尝试获取主类名
        if (cu.getPrimaryTypeName().isPresent()) {
            className = cu.getPrimaryTypeName().get();
        } else {
            // 如果没有主类名，尝试获取第一个类型声明
            if (!cu.getTypes().isEmpty()) {
                className = cu.getTypes().get(0).getNameAsString();
            }
        }
        
        if (className == null || className.isEmpty()) {
            logger.warn("解析失败，无法获取类名: {}", javaFile.getPath());
            throw new IOException("无法获取类名");
        }
        
        if (cu.getPackageDeclaration().isPresent()) {
            String packageName = cu.getPackageDeclaration().get().getNameAsString();
            className = packageName + "." + className;
        }
        
        sourceCodeCache.put(className, sourceCode);
        logger.info("解析成功: {} -> {}", className, javaFile.getPath());
    }
    
    public ExtractResult uploadAndExtractFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        logger.info("开始处理上传文件: {}", originalFilename);
        
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path targetFile = uploadPath.resolve(originalFilename);
        Files.copy(file.getInputStream(), targetFile);
        
        logger.info("文件已保存: {}", targetFile);
        
        String extractPath = uploadPath.resolve(extractBaseName(originalFilename)).toString();
        
        ExtractResult result = new ExtractResult();
        result.setExtractPath(extractPath);
        
        try {
            if (originalFilename.endsWith(".zip")) {
                result = extractZipFile(targetFile.toFile(), extractPath);
            } else if (originalFilename.endsWith(".tar.gz") || originalFilename.endsWith(".tgz")) {
                result = extractTarGzFile(targetFile.toFile(), extractPath);
            } else {
                throw new IllegalArgumentException("不支持的文件类型，仅支持 .zip 或 .tar.gz 文件");
            }
        } catch (IOException e) {
            logger.error("解压文件失败: {}, 错误: {}", originalFilename, e.getMessage(), e);
            throw e;
        }
        
        result.setExtractPath(extractPath);
        logger.info("文件上传并解压完成: {}, {}", extractPath, result.getSummary());
        return result;
    }
    
    private ExtractResult extractZipFile(File zipFile, String extractPath) throws IOException {
        Path destPath = Paths.get(extractPath);
        if (!Files.exists(destPath)) {
            Files.createDirectories(destPath);
        }
        
        Charset[] charsets = {StandardCharsets.UTF_8, Charset.forName("GBK"), Charset.forName("GB2312"), Charset.forName("ISO-8859-1")};
        IOException lastException = null;
        
        for (Charset charset : charsets) {
            logger.info("尝试使用编码 {} 解压 ZIP 文件: {}", charset, zipFile.getName());
            
            ExtractResult result = new ExtractResult();
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), charset)) {
                ZipEntry entry;
                
                while ((entry = zis.getNextEntry()) != null) {
                    try {
                        String entryName = entry.getName();
                        logger.debug("处理条目: {}", entryName);
                        
                        Path entryPath = destPath.resolve(entryName);
                        
                        if (entry.isDirectory()) {
                            Files.createDirectories(entryPath);
                            result.addSuccess();
                        } else {
                            try {
                                if (entryPath.getParent() != null && !Files.exists(entryPath.getParent())) {
                                    Files.createDirectories(entryPath.getParent());
                                }
                                Files.copy(zis, entryPath);
                                result.addSuccess();
                            } catch (IOException e) {
                                logger.warn("解压文件失败: {}, 错误: {}", entryName, e.getMessage());
                                result.addFailure(entryName);
                            }
                        }
                        zis.closeEntry();
                    } catch (Exception e) {
                        logger.warn("处理条目时发生异常: {}, 错误: {}", entry.getName(), e.getMessage());
                        result.addFailure(entry.getName());
                        zis.closeEntry();
                    }
                }
                
                if (result.getSuccessCount() > 0 || result.getFailureCount() > 0) {
                    logger.info("ZIP文件解压完成: {} -> {} (使用编码: {}, {})", 
                               zipFile.getPath(), extractPath, charset, result.getSummary());
                    return result;
                }
            } catch (IllegalArgumentException e) {
                logger.warn("编码 {} 不适用: {}", charset, e.getMessage());
                lastException = new IOException("编码 " + charset + " 解析失败", e);
            }
        }
        
        throw new IOException("ZIP文件解压失败，尝试了多种编码均失败", lastException);
    }
    
    private ExtractResult extractTarGzFile(File tarGzFile, String extractPath) throws IOException {
        Path destPath = Paths.get(extractPath);
        if (!Files.exists(destPath)) {
            Files.createDirectories(destPath);
        }
        
        Charset[] charsets = {StandardCharsets.UTF_8, Charset.forName("GBK"), Charset.forName("GB2312"), Charset.forName("ISO-8859-1")};
        IOException lastException = null;
        
        for (Charset charset : charsets) {
            logger.info("尝试使用编码 {} 解压 TAR.GZ 文件: {}", charset, tarGzFile.getName());
            
            ExtractResult result = new ExtractResult();
            
            try (InputStream fi = new FileInputStream(tarGzFile);
                 InputStream bi = new BufferedInputStream(fi);
                 InputStream gzi = new GzipCompressorInputStream(bi);
                 TarArchiveInputStream tis = new TarArchiveInputStream(gzi, String.valueOf(charset))) {
                
                TarArchiveEntry entry;
                
                while ((entry = tis.getNextTarEntry()) != null) {
                    try {
                        String entryName = entry.getName();
                        logger.debug("处理条目: {}", entryName);
                        
                        Path entryPath = destPath.resolve(entryName);
                        
                        if (entry.isDirectory()) {
                            Files.createDirectories(entryPath);
                            result.addSuccess();
                        } else {
                            try {
                                if (entryPath.getParent() != null && !Files.exists(entryPath.getParent())) {
                                    Files.createDirectories(entryPath.getParent());
                                }
                                Files.copy(tis, entryPath);
                                result.addSuccess();
                            } catch (IOException e) {
                                logger.warn("解压文件失败: {}, 错误: {}", entryName, e.getMessage());
                                result.addFailure(entryName);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("处理条目时发生异常: {}, 错误: {}", entry.getName(), e.getMessage());
                        result.addFailure(entry.getName());
                    }
                }
                
                if (result.getSuccessCount() > 0 || result.getFailureCount() > 0) {
                    logger.info("TAR.GZ文件解压完成: {} -> {} (使用编码: {}, {})", 
                               tarGzFile.getPath(), extractPath, charset, result.getSummary());
                    return result;
                }
            } catch (IllegalArgumentException e) {
                logger.warn("编码 {} 不适用: {}", charset, e.getMessage());
                lastException = new IOException("编码 " + charset + " 解析失败", e);
            }
        }
        
        throw new IOException("TAR.GZ文件解压失败，尝试了多种编码均失败", lastException);
    }
    
    private String extractBaseName(String filename) {
        if (filename.endsWith(".tar.gz")) {
            return filename.substring(0, filename.length() - 7);
        } else if (filename.endsWith(".tgz")) {
            return filename.substring(0, filename.length() - 4);
        } else if (filename.endsWith(".zip")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }
}
