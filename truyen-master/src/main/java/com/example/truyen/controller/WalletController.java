package com.example.truyen.controller;

import com.example.truyen.dto.response.ApiResponse;
import com.example.truyen.dto.response.WalletResponse;
import com.example.truyen.dto.response.WalletTransactionResponse;
import com.example.truyen.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // Xem ví của tôi (yêu cầu đăng nhập)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet() {
        WalletResponse wallet = walletService.getMyWallet();
        return ResponseEntity.ok(ApiResponse.success("Get wallet successfully", wallet));
    }

    // Cộng xu trực tiếp cho user (Admin, Super Admin) - dùng để test hoặc tặng xu
    @PostMapping("/add-coins")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> addCoins(
            @RequestParam Long userId,
            @RequestParam int amount,
            @RequestParam(defaultValue = "Admin top-up") String description) {
        walletService.addCoins(userId, amount, description, null);
        return ResponseEntity.ok(ApiResponse.success("Added " + amount + " coins to user " + userId, null));
    }

    // Xem lịch sử giao dịch của tôi (yêu cầu đăng nhập)
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<WalletTransactionResponse> transactions = walletService.getMyTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success("Get transactions successfully", transactions));
    }
}
