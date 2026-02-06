package com.trae.loganalysis.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class SourceCodeParserService implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(SourceCodeParserService.class);
    
    @Autowired
    private SourceCodeCache sourceCodeCache;
    
    @Value("${source-code.directories}")
    private String directoriesConfig;
    
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
}
