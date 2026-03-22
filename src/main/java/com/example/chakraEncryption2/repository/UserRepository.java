package com.example.chakraEncryption2.repository;

import com.example.chakraEncryption2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByEmailStartingWithIgnoreCase(String email);
}