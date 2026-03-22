package com.example.chakraEncryption2.repository;

import com.example.chakraEncryption2.entity.FilePermission;
import com.example.chakraEncryption2.entity.EncryptedFile;
import com.example.chakraEncryption2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<FilePermission, Long> {

    // ⭐ Controller-la call panna indha methods venum
    List<FilePermission> findAllByFile(EncryptedFile file);

    List<FilePermission> findAllByAllowedUser(User allowedUser);

    // Old methods (keep them if needed)
    List<FilePermission> findByFile(EncryptedFile file);
    List<FilePermission> findByAllowedUser(User allowedUser);

    long countByFile(EncryptedFile file);
    boolean existsByFileAndAllowedUser(EncryptedFile file, User allowedUser);
    Optional<FilePermission> findByFileAndAllowedUser(EncryptedFile file, User allowedUser);

}