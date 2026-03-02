package com.example.truyen.service.impl;

import com.example.truyen.dto.response.WalletResponse;
import com.example.truyen.dto.response.WalletTransactionResponse;
import com.example.truyen.entity.User;
import com.example.truyen.entity.UserWallet;
import com.example.truyen.entity.WalletTransaction;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.repository.UserWalletRepository;
import com.example.truyen.repository.WalletTransactionRepository;
import com.example.truyen.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public WalletResponse getMyWallet() {
        User user = getCurrentUser();
        UserWallet wallet = getOrCreateWallet(user);
        return WalletResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .balance(wallet.getBalance())
                .build();
    }

    @Transactional
    @Override
    public void addCoins(Long userId, int amount, String description, Long refId) {
        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return walletRepository.save(UserWallet.builder().user(user).balance(0).build());
                });

        int newBalance = wallet.getBalance() + amount;
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        saveTransaction(wallet.getUser(), WalletTransaction.Type.DEPOSIT, amount, newBalance, description, refId);
    }

    @Transactional
    @Override
    public void spendCoins(Long userId, int amount, String description, Long refId) {
        UserWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));

        if (wallet.getBalance() < amount) {
            throw new BadRequestException(
                    "Insufficient coins. Required: " + amount + ", available: " + wallet.getBalance());
        }

        int newBalance = wallet.getBalance() - amount;
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        saveTransaction(wallet.getUser(), WalletTransaction.Type.SPEND, amount, newBalance, description, refId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<WalletTransactionResponse> getMyTransactions(Pageable pageable) {
        User user = getCurrentUser();
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toTransactionResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public WalletResponse getWalletByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        UserWallet wallet = getOrCreateWallet(user);
        return WalletResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .balance(wallet.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<WalletTransactionResponse> getTransactionsByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toTransactionResponse);
    }

    private void saveTransaction(User user, WalletTransaction.Type type, int amount, int balanceAfter,
            String description, Long refId) {
        WalletTransaction tx = WalletTransaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .refId(refId)
                .build();
        transactionRepository.save(tx);
    }

    private UserWallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserWallet wallet = UserWallet.builder().user(user).build();
            return walletRepository.save(wallet);
        });
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
