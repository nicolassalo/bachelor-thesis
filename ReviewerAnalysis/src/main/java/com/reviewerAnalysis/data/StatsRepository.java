package com.reviewerAnalysis.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatsRepository extends JpaRepository<Stats, Long> {
    void deleteByLang(String lang);
    List<Stats> findByLang(String lang);
}
