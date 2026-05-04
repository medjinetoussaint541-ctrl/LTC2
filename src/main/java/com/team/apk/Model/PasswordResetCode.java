package com.team.apk.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant un code temporaire de réinitialisation de mot de passe.
 * Suit la même structure que EmailVerificationCode pour garder la cohérence.
 */
@Entity
@Table(name = "PASSWORD_RESET_CODES")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_reset_seq_gen")
    @SequenceGenerator(
            name = "password_reset_seq_gen",
            sequenceName = "SEQ_PASSWORD_RESET_CODES",
            allocationSize = 1
    )
    @Column(name = "RESETID")
    private Long resetId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "USERID",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "FK_PASSWORD_RESET_USER")
    )
    private Users user;

    @Column(name = "RESETCODE", nullable = false, length = 10)
    private String resetCode;

    @Column(name = "CREATEDAT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "LASTGENERATEDAT", nullable = false)
    private LocalDateTime lastGeneratedAt;

    @Column(name = "EXPIRESAT", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastGeneratedAt == null) lastGeneratedAt = now;
    }

    public Long getResetId() { return resetId; }
    public void setResetId(Long resetId) { this.resetId = resetId; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public String getResetCode() { return resetCode; }
    public void setResetCode(String resetCode) { this.resetCode = resetCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastGeneratedAt() { return lastGeneratedAt; }
    public void setLastGeneratedAt(LocalDateTime lastGeneratedAt) { this.lastGeneratedAt = lastGeneratedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
