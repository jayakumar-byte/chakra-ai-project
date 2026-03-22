package com.example.chakraEncryption2.repository;

import com.example.chakraEncryption2.entity.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    // 🔴 RED ALERT: For File Owners
    List<Anomaly> findByOwnerIdAndRedAlertTrueAndReportedFalseOrderByTimestampDesc(Long ownerId);

    // 📊 ADMIN STATS
    long countByRedAlertTrue();
    long countByYellowAlertTrue();

    // 📩 REPORTING: Ippo logic-ah mathiytom!
    // Report section-la dismiss pannaadha items-ah mattum fetch panna:
    List<Anomaly> findByReportedTrueAndDismissedInReportFalse();

    long countByReportedTrue();

    // 📋 GLOBAL AUDIT: Log eppovumae irukkum (Dismissed records-um sethu kaatum)
    List<Anomaly> findAllByOrderByTimestampDesc();

    // 🔍 OWNER VIEW
    List<Anomaly> findByOwnerId(Long ownerId);

    // 🛡️ ANTI-SPAM
    boolean existsByAttemptedByAndFilenameAndTimestampAfter(String attemptedBy, String filename, LocalDateTime timestamp);
}