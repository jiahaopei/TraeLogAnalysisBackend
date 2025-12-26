package com.trae.loganalysis.repository;

import com.trae.loganalysis.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    List<AnalysisResult> findByFileId(Long fileId);
    List<AnalysisResult> findByFileDataId(Long fileDataId);
    <S extends AnalysisResult> List<S> saveAll(Iterable<S> entities);
}
