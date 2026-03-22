package com.example.chakraEncryption2.repository;

import com.example.chakraEncryption2.entity.FileActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileActivityRepository extends JpaRepository<FileActivity, Long> {

    // Oru specific file-oda history paaka
    List<FileActivity> findByFilenameOrderByAccessTimeDesc(String filename);

    // Ellaa file activities-aiyum latest-aa edukka
    List<FileActivity> findAllByOrderByAccessTimeDesc();

    // Specific user enna pannaanga nu paaka
    List<FileActivity> findByAccessedBy(String email);
}