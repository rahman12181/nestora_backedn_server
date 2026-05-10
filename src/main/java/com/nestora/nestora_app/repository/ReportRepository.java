package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(String status);
    List<Report> findByTypeAndRefId(Report.ReportType type, Long refId);
}
