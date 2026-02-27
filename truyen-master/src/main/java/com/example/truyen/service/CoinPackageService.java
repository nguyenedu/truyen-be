package com.example.truyen.service;

import com.example.truyen.dto.request.CoinPackageRequest;
import com.example.truyen.dto.response.CoinPackageResponse;

import java.util.List;

public interface CoinPackageService {

    List<CoinPackageResponse> getAllActivePackages();

    CoinPackageResponse getById(Long id);

    CoinPackageResponse create(CoinPackageRequest request);

    CoinPackageResponse update(Long id, CoinPackageRequest request);

    void delete(Long id);
}
