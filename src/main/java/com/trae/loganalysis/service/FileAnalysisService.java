package com.trae.loganalysis.service;

import com.alibaba.fastjson.JSONObject;
import com.trae.loganalysis.entity.AnalysisResult;
import com.trae.loganalysis.entity.FileData;
import com.trae.loganalysis.entity.UploadFile;
import com.trae.loganalysis.model.SourceCodeInfo;
import com.trae.loganalysis.repository.AnalysisResultRepository;
import com.trae.loganalysis.repository.FileDataRepository;
import com.trae.loganalysis.repository.UploadFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class FileAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(FileAnalysisService.class);
    private final UploadFileRepository uploadFileRepository;
    private final FileDataRepository fileDataRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final RestTemplate restTemplate;

    private final ExecutorService executorService;
    
    // API Configuration
    @Value("${api.log-analysis.url}")
    private String logAnalysisUrl;
    
    @Value("${api.log-analysis.system-code}")
    private String logAnalysisSystemCode;
    
    @Value("${api.log-analysis.condition.key}")
    private String logAnalysisConditionKey;
    
    @Value("${api.log-analysis.condition.value}")
    private String logAnalysisConditionValue;
    
    @Value("${api.log-analysis.size}")
    private int logAnalysisSize;
    
    // AI Suggestion Configuration
    @Value("${api.ai-suggestion.url}")
    private String aiSuggestionUrl;
    
    @Value("${api.ai-suggestion.system-code}")
    private String aiSuggestionSystemCode;
    
    @Value("${api.ai-suggestion.query-params.data-set-id}")
    private int aiSuggestionDataSetId;
    
    @Value("${api.ai-suggestion.query-params.app-id}")
    private int aiSuggestionAppId;
    
    @Value("${api.ai-suggestion.query-params.index-prefix}")
    private String aiSuggestionIndexPrefix;
    
    @Value("${api.ai-suggestion.query-params.size}")
    private int aiSuggestionSize;
    
    @Value("${api.ai-suggestion.query-params.remark}")
    private String aiSuggestionRemark;

    public FileAnalysisService(UploadFileRepository uploadFileRepository,
                              FileDataRepository fileDataRepository,
                              AnalysisResultRepository analysisResultRepository,
                              RestTemplate restTemplate,
                              @Value("${file.analysis.thread-pool-size}") int threadPoolSize) {
        this.uploadFileRepository = uploadFileRepository;
        this.fileDataRepository = fileDataRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.restTemplate = restTemplate;
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

                // 批量分析数据行
                List<CompletableFuture<AnalysisResult>> futures = fileDataList.stream()
                        .map(fileData -> CompletableFuture.supplyAsync(
                                () -> analyzeDataRow(fileId, fileData), executorService))
                        .collect(Collectors.toList());

                // 等待所有分析完成
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));

                // 收集所有分析结果
                List<AnalysisResult> results = allFutures.thenApply(v ->
                        futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()))
                        .join();

                // 批量保存分析结果
                if (!results.isEmpty()) {
                    analysisResultRepository.saveAll(results);
                }

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
     * @return 分析结果
     */
    private AnalysisResult analyzeDataRow(Long fileId, FileData fileData) {
        AnalysisResult result = new AnalysisResult();
        result.setFileId(fileId);
        result.setFileDataId(fileData.getId());
        result.setAnalysisTime(new Date());
        result.setStatus("SUCCESS");

        try {
            // Step 1: Call logAnalysis API to get log info
            String logInfoResponse = callLogAnalysisApi(fileData.getColumn4());
            
            // Parse JSON response using Fastjson
            JSONObject rootObj = JSONObject.parseObject(logInfoResponse);
            String retCode = rootObj.getString("retCode");
            
            // Check if API call was successful
            String logMessage = "";
            if (!"0000".equals(retCode)) {
                // API call failed, set result status to FAILED
                logMessage  = "获取日志失败";
            }else {
                JSONObject entityObj = rootObj.getJSONObject("entity");
                if (entityObj != null) {
                    // Extract values array from entity
                    java.util.List<JSONObject> valuesArray = entityObj.getJSONArray("values").toJavaList(JSONObject.class);
                    if (!valuesArray.isEmpty()) {
                        // Get first value object
                        JSONObject firstValueObj = valuesArray.get(0);
                        // Extract source object
                        JSONObject sourceObj = firstValueObj.getJSONObject("source");
                        if (sourceObj != null) {
                            // Get @message field
                            logMessage = sourceObj.getString("@message");
                        }
                    }
                }
            }
            result.setLogInfo(logMessage);

            // Step 2: Extract method name from log message
            String methodName = extractMethodNameFromLog(logMessage);
            
            // Step 3: Call source code API to get full source code
            SourceCodeInfo sourceCodeInfo = callSourceCodeApi(fileData.getColumn4());
            
            // Step 4: Extract only method code from full source code
            String methodCode = extractMethodCode(sourceCodeInfo.getSourceCode(), sourceCodeInfo.getClassName(), 
                                            sourceCodeInfo.getLineNum(), sourceCodeInfo.getMethodName());
            
            // Set source code information to result
            result.setClassName(sourceCodeInfo.getClassName());
            result.setLineNumber(sourceCodeInfo.getLineNum());
            result.setMethodName(sourceCodeInfo.getMethodName());
            result.setCode(methodCode);
            
            // Step 5: Call AI suggestion API with method code
            String aiSuggestionResponse = callAiSuggestionApi(methodCode);
            result.setResultContent(aiSuggestionResponse);

        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setResultContent("Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
    
    /**
     * Extract method name from log message
     * Log format: [YYYY-MM-DD HH:mm:ss][ERROR][xxx-xxx][][][org.spring.conftig.updataCommon:98][Thread][td][sd]...
     * Uses regex to find method name regardless of bracket count before or after
     */
    private String extractMethodNameFromLog(String logMessage) {
        if (logMessage == null || logMessage.isEmpty()) {
            return "";
        }
        
        // Regex pattern to match [fully.qualified.Class.method:lineNumber]
        // Looks for content like [org.spring.conftig.updataCommon:98] and extracts method name
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[([\\w.]+:[\\d]+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(logMessage);
        
        if (matcher.find()) {
            // Extract content inside brackets, e.g., "org.spring.conftig.updataCommon:98"
            String bracketedContent = matcher.group(1);
            // Split on colon to get class.method part and line number
            String[] parts = bracketedContent.split(":");
            if (parts.length > 0) {
                String classMethodPart = parts[0];
                // Split on dots to get all parts
                String[] classMethodParts = classMethodPart.split("\\.");
                if (classMethodParts.length > 0) {
                    // The method name is last part
                    return classMethodParts[classMethodParts.length - 1];
                }
            }
        }
        
        return "";
    }
    
    /**
     * Call source code API to get full Java source code
     * 调用外部接口返回对象格式为{"data":[{"className":"cn.com.handler","lineNum":150,"methodName":"socketHandler","sourceCode":"..."}]}
     */
    private SourceCodeInfo callSourceCodeApi(String column4) {
        SourceCodeInfo sourceCodeInfo = new SourceCodeInfo();
        
        try {
            // Create request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request body
            String requestBody = String.format(
                "{\"column4\":\"%s\"}",
                column4
            );
            
            // Create HttpEntity with headers and body
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Send POST request to source code API
            ResponseEntity<String> response = restTemplate.exchange(
                    "${api.source-code.url}", 
                    HttpMethod.POST, 
                    requestEntity, 
                    String.class);
            
            // Parse JSON response
            String responseBody = response.getBody();
            JSONObject rootObj = JSONObject.parseObject(responseBody);
            String retCode = rootObj.getString("retCode");
            
            if ("0000".equals(retCode)) {
                JSONObject entityObj = rootObj.getJSONObject("entity");
                if (entityObj != null) {
                    // Extract data array
                    com.alibaba.fastjson.JSONArray dataArray = entityObj.getJSONArray("data");
                    if (dataArray != null && !dataArray.isEmpty()) {
                        // Get first data object
                        JSONObject firstDataObj = dataArray.getJSONObject(0);
                        sourceCodeInfo.setSourceCode(firstDataObj.getString("sourceCode"));
                        sourceCodeInfo.setClassName(firstDataObj.getString("className"));
                        sourceCodeInfo.setMethodName(firstDataObj.getString("methodName"));
                        sourceCodeInfo.setLineNum(firstDataObj.getInteger("lineNum"));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("调用源码API失败", e);
        }
        
        return sourceCodeInfo;
    }
    
    /**
     * Extract only specific method code from full source code
     * 根据methodName和lineNum将sourceCode中methodName对应的源码抽取出来
     * 并按照指定格式输出到日志
     */
    private String extractMethodCode(String fullSourceCode, String className, Integer lineNum, String methodName) {
        if (fullSourceCode == null || fullSourceCode.isEmpty() || methodName == null || methodName.isEmpty()) {
            return fullSourceCode;
        }
        
        // Find method signature - simplified pattern to match method with any return type
        String methodSignaturePattern = "\\b" + methodName + "\\s*\\([^)]*\\)\\s*\\{";
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(methodSignaturePattern, java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(fullSourceCode);
        
        if (!matcher.find()) {
            logger.warn("未找到方法签名: {}", methodName);
            return fullSourceCode;
        }
        
        int startIndex = matcher.start();
        int braceCount = 0;
        int endIndex = startIndex;
        boolean insideString = false;
        
        // Find matching closing brace
        for (int i = startIndex; i < fullSourceCode.length(); i++) {
            char c = fullSourceCode.charAt(i);
            
            // Check for string literals to avoid counting braces inside strings
            if (c == '"' && (i == 0 || fullSourceCode.charAt(i - 1) != '\\')) {
                insideString = !insideString;
            }
            
            if (!insideString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i + 1;
                        break;
                    }
                }
            }
        }
        
        String extractedCode = fullSourceCode.substring(startIndex, endIndex);
        
        // 按照指定格式输出到日志
        logger.info("抽取的方法源码：\n/**\n * 类名：{}\n * 方法名：{}\n * 行号：{}\n */\n{}", 
                   className, methodName, lineNum, extractedCode);
        
        return extractedCode;
    }
    
    /**
     * Call log analysis API to get log information
     */
    private String callLogAnalysisApi(String column4) {
        // Create request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String logAnalysisEndTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Date startDate = new Date(new Date().getTime() - 24 * 60 * 60 * 1000);
        String logAnalysisStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startDate);
        // Create request body using configuration parameters
        String requestBody = String.format(
            "{\"systemCode\":\"%s\",\"message\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"conditionValueMap\":{\"condition\":\"%s\",\"value\":\"%s\"},\"size\":%d}",
            logAnalysisSystemCode, column4, logAnalysisStartTime, logAnalysisEndTime,
            logAnalysisConditionKey, logAnalysisConditionValue, logAnalysisSize
        );
        
        // Create HttpEntity with headers and body
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        
        // Send POST request using configured URL
        ResponseEntity<String> response = restTemplate.exchange(
                logAnalysisUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class);
        
        return response.getBody();
    }
    
    
    /**
     * Call AI suggestion API with log information
     */
    private String callAiSuggestionApi(String logMessage) throws IOException {
        // Construct API URL with query parameter using configuration
        String apiUrl = aiSuggestionUrl + "?systemCode=" + aiSuggestionSystemCode;
        
        // Create request body using configuration parameters
        String requestBody = String.format(
            "{\"queryCondiion\":\"\",\"querySource\":[{\"dataSetId\":%d,\"centerIds\":[4343],\"dataSetAlias\":null,\"appId\":%d}],\"indexPrefix\":\"%s\",\"options\":{\"sortBy\":[{\"@rownumber\":\"asc\"}],\"size\":%d,\"remark\":\"%s\",\"format\":\"std\",\"highlight\":false,\"trackTotalHits\":false},\"time_zone\":\"+8:00\"}",
            aiSuggestionDataSetId, aiSuggestionAppId, aiSuggestionIndexPrefix, aiSuggestionSize, aiSuggestionRemark
        );
        
        // Use HttpURLConnection to handle text/event-stream response
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Write request body
        conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
        
        // Read text/event-stream response
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }
        }
        
        conn.disconnect();
        return responseBuilder.toString();
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