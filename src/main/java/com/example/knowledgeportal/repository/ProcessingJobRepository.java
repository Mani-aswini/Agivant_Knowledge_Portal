package com.example.knowledgeportal.repository;

import com.example.knowledgeportal.entity.IndexingStatus;
import com.example.knowledgeportal.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, Long> {

    List<ProcessingJob> findAllByOrderByStartedAtDesc();

    List<ProcessingJob> findTop50ByOrderByStartedAtDesc();

    List<ProcessingJob> findByStatus(IndexingStatus status);

    long countByStatus(IndexingStatus status);
}
