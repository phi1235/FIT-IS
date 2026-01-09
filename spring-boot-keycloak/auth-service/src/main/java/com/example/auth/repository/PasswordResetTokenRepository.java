package com.example.auth.repository;

import com.example.auth.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    
    Optional<PasswordResetToken> findByIdAndNotConsumed(UUID id);
    
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user.id = :userId " +
           "AND t.tokenType = :tokenType AND t.createdAt > :after")
    long countRecentAttempts(@Param("userId") UUID userId, 
                            @Param("tokenType") PasswordResetToken.TokenType tokenType,
                            @Param("after") LocalDateTime after);
    
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.consumed = true, t.consumedAt = CURRENT_TIMESTAMP " +
           "WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.consumed = false AND t.verified = false")
    void invalidateUnverifiedTokens(@Param("userId") UUID userId, 
                                   @Param("tokenType") PasswordResetToken.TokenType tokenType);
    
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user.id = :userId " +
           "AND t.tokenType IN ('FORGOT_PASSWORD', 'CHANGE_PASSWORD') " +
           "AND t.createdAt > :startTime AND t.createdAt <= :endTime")
    long countResetAttemptsInTimeWindow(@Param("userId") UUID userId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
