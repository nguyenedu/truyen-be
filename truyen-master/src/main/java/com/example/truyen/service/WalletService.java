package com.example.truyen.service;

import com.example.truyen.dto.response.WalletResponse;
import com.example.truyen.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    WalletResponse getMyWallet();

    void addCoins(Long userId, int amount, String description, Long refId);

    void spendCoins(Long userId, int amount, String description, Long refId);

    Page<WalletTransactionResponse> getMyTransactions(Pageable pageable);
}
