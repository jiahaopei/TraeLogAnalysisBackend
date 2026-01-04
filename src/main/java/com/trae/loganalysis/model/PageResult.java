package com.trae.loganalysis.model;

import java.util.List;

/**
 * 分页结果封装类，用于返回包含分页信息的响应
 */
public class PageResult<T> {
    
    /**
     * 当前页码
     */
    private int page;
    
    /**
     * 每页大小
     */
    private int size;
    
    /**
     * 总条数
     */
    private long total;
    
    /**
     * 总页数
     */
    private int totalPages;
    
    /**
     * 数据列表
     */
    private List<T> data;
    
    /**
     * 构造方法
     * @param page 当前页码
     * @param size 每页大小
     * @param total 总条数
     * @param data 数据列表
     */
    public PageResult(int page, int size, long total, List<T> data) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.data = data;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
    
    // getter and setter methods
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / this.size);
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
}