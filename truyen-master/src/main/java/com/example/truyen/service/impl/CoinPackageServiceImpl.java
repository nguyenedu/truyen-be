package com.example.truyen.service.impl;

import com.example.truyen.dto.request.CoinPackageRequest;
import com.example.truyen.dto.response.CoinPackageResponse;
import com.example.truyen.entity.CoinPackage;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.CoinPackageRepository;
import com.example.truyen.service.CoinPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoinPackageServiceImpl implements CoinPackageService {

    private final CoinPackageRepository coinPackageRepository;

    @Transactional(readOnly = true)
    @Override
    public List<CoinPackageResponse> getAllActivePackages() {
        return coinPackageRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CoinPackageResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    @Override
    public CoinPackageResponse create(CoinPackageRequest request) {
        CoinPackage pkg = CoinPackage.builder()
                .name(request.getName())
                .coins(request.getCoins())
                .bonusCoins(request.getBonusCoins())
                .price(request.getPrice())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return toResponse(coinPackageRepository.save(pkg));
    }

    @Transactional
    @Override
    public CoinPackageResponse update(Long id, CoinPackageRequest request) {
        CoinPackage pkg = findOrThrow(id);
        pkg.setName(request.getName());
        pkg.setCoins(request.getCoins());
        pkg.setBonusCoins(request.getBonusCoins());
        pkg.setPrice(request.getPrice());
        if (request.getIsActive() != null)
            pkg.setIsActive(request.getIsActive());
        return toResponse(coinPackageRepository.save(pkg));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        CoinPackage pkg = findOrThrow(id);
        pkg.setIsActive(false);
        coinPackageRepository.save(pkg);
    }

    private CoinPackage findOrThrow(Long id) {
        return coinPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CoinPackage", "id", id));
    }

    private CoinPackageResponse toResponse(CoinPackage pkg) {
        return CoinPackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .coins(pkg.getCoins())
                .bonusCoins(pkg.getBonusCoins())
                .price(pkg.getPrice())
                .isActive(pkg.getIsActive())
                .build();
    }
}
