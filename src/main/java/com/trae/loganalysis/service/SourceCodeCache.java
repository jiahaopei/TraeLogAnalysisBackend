package com.trae.loganalysis.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SourceCodeCache {
    
    private final Map<String, String> sourceCodeMap = new ConcurrentHashMap<>();
    private final Map<String, String> classNameToSourceCodeMap = new ConcurrentHashMap<>();
    
    public void put(String className, String sourceCode) {
        sourceCodeMap.put(className, sourceCode);
        classNameToSourceCodeMap.put(className, sourceCode);
    }
    
    public String getSourceCode(String className) {
        return sourceCodeMap.get(className);
    }
    
    public boolean contains(String className) {
        return sourceCodeMap.containsKey(className);
    }
    
    public int size() {
        return sourceCodeMap.size();
    }
    
    public void clear() {
        sourceCodeMap.clear();
        classNameToSourceCodeMap.clear();
    }
}
