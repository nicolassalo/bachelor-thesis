package com.reviewerAnalysis.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordRepository extends JpaRepository<Password, Long> {
    Boolean existsByPassword(String password);
}
