package com.example.truyen.repository;

import com.example.truyen.entity.CoinPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinPackageRepository extends JpaRepository<CoinPackage, Long> {

    List<CoinPackage> findByIsActiveTrue();
}
