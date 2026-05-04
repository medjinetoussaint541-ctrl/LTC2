/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team.apk.Model;

/**
 *
 * @author HP
 */

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMAIL_VERIFICATION_CODES")
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_verification_seq_gen")
    @SequenceGenerator(
            name = "email_verification_seq_gen",
            sequenceName = "SEQ_EMAIL_VERIFICATION_CODES",
            allocationSize = 1
    )
    @Column(name = "VERIFICATIONID")
    private Long verificationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "USERID",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "FK_EMAIL_VERIFICATION_USER")
    )
    private Users user;

    @Column(name = "VERIFICATIONCODE", nullable = false, length = 10)
    private String verificationCode;

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

    public Long getVerificationId() { return verificationId; }
    public void setVerificationId(Long verificationId) { this.verificationId = verificationId; }

    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastGeneratedAt() { return lastGeneratedAt; }
    public void setLastGeneratedAt(LocalDateTime lastGeneratedAt) { this.lastGeneratedAt = lastGeneratedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
