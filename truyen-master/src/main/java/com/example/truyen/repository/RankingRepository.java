package com.example.truyen.repository;

import com.example.truyen.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {


    @Query("SELECT r FROM Ranking r WHERE r.rankingType = :type AND r.rankingDate = :date ORDER BY r.rankPosition ASC")
    List<Ranking> findByRankingTypeAndDate(
            @Param("type") Ranking.RankingType type,
            @Param("date") LocalDate date
    );


    @Query("SELECT DISTINCT r FROM Ranking r " +
            "JOIN FETCH r.story s " +
            "LEFT JOIN FETCH s.author " +
            "LEFT JOIN FETCH s.categories " +
            "WHERE r.rankingType = :type " +
            "ORDER BY r.rankingDate DESC, r.rankPosition ASC")
    List<Ranking> findLatestByRankingType(@Param("type") Ranking.RankingType type);

    @Modifying
    @Query("DELETE FROM Ranking r WHERE r.rankingType = :type AND r.rankingDate = :date")
    void deleteByRankingTypeAndDate(@Param("type") Ranking.RankingType type, @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM Ranking r WHERE r.rankingDate < :beforeDate")
    void deleteOlderThan(@Param("beforeDate") LocalDate beforeDate);
}