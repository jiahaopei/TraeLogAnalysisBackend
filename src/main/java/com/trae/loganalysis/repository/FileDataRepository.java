package com.trae.loganalysis.repository;

import com.trae.loganalysis.entity.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileDataRepository extends JpaRepository<FileData, Long> {
    List<FileData> findByFileId(Long fileId);
}
