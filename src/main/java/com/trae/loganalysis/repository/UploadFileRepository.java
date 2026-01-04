package com.trae.loganalysis.repository;

import com.trae.loganalysis.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {
    
    /**
     * 使用SQLite兼容的LIMIT OFFSET语法分页查询所有上传文件
     * @param offset 偏移量
     * @param limit 每页大小
     * @return 上传文件列表
     */
    @Query(value = "SELECT * FROM upload_file ORDER BY id ASC LIMIT ?2 OFFSET ?1", nativeQuery = true)
    List<UploadFile> findAllByOrderByIdAsc(int offset, int limit);
    
    /**
     * 获取上传文件总条数
     * @return 总条数
     */
    @Query(value = "SELECT COUNT(*) FROM upload_file", nativeQuery = true)
    long countAllUploadFiles();
}
