package com.example.chakraEncryption2.repository;

import com.example.chakraEncryption2.entity.EncryptedFile;
import com.example.chakraEncryption2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<EncryptedFile, Long> {

    // History page-ku
    List<EncryptedFile> findByOwnerAndHiddenFromUserFalse(User owner);

    // ⭐ IDHU THAAN MUKKIYAM: Filename maathunaalum file-ah kandupudikka
    Optional<EncryptedFile> findByFileHash(String fileHash);

    List<EncryptedFile> findAllByOwner(User owner);
}