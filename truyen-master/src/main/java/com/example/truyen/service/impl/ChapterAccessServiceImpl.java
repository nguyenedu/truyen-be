package com.example.truyen.service.impl;

import com.example.truyen.entity.Chapter;
import com.example.truyen.entity.User;
import com.example.truyen.entity.UserChapterAccess;
import com.example.truyen.exception.BadRequestException;
import com.example.truyen.exception.ResourceNotFoundException;
import com.example.truyen.repository.ChapterRepository;
import com.example.truyen.repository.UserChapterAccessRepository;
import com.example.truyen.repository.UserRepository;
import com.example.truyen.service.ChapterAccessService;
import com.example.truyen.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChapterAccessServiceImpl implements ChapterAccessService {

    private final UserChapterAccessRepository accessRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    @Transactional(readOnly = true)
    @Override
    public boolean hasAccess(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        if (!chapter.getIsLocked())
            return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return false;
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            return true;
        }

        return accessRepository.existsByUserIdAndChapterId(user.getId(), chapterId);
    }

    @Transactional
    @Override
    public void unlockChapter(Long chapterId) {
        User user = getCurrentUser();

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", "id", chapterId));

        if (!chapter.getIsLocked()) {
            throw new BadRequestException("This chapter is free to read");
        }

        if (accessRepository.existsByUserIdAndChapterId(user.getId(), chapterId)) {
            throw new BadRequestException("You already unlocked this chapter");
        }

        walletService.spendCoins(
                user.getId(),
                chapter.getCoinsPrice(),
                "Mo khoa chuong " + chapter.getChapterNumber() + " - " + chapter.getStory().getTitle(),
                chapterId);

        UserChapterAccess access = UserChapterAccess.builder()
                .user(user)
                .chapter(chapter)
                .coinsSpent(chapter.getCoinsPrice())
                .build();

        accessRepository.save(access);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
