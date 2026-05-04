/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team.apk.Service;

/**
 *
 * @author HP
 */

import com.team.apk.Model.EmailVerificationCode;
import com.team.apk.Model.Users;
import com.team.apk.Repository.EmailVerificationCodeRepository;
import com.team.apk.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service qui gère tout le cycle de vie des codes de vérification :
 * génération, régénération, validation et expiration.
 */
@Service
public class VerificationCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final UsersRepository usersRepository;
    private final int expirationMinutes;

    public VerificationCodeService(EmailVerificationCodeRepository emailVerificationCodeRepository,
                                   UsersRepository usersRepository,
                                   @Value("${app.email-verification.code-expiration-minutes:10}") int expirationMinutes) {
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.usersRepository = usersRepository;
        this.expirationMinutes = expirationMinutes;
    }
    
//    Génère un nouveau code ou remplace l'ancien pour l'utilisateur donné.
    @Transactional
    public String generateOrRefreshCode(Users user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Cet email est déjà vérifié.");
        }

        EmailVerificationCode verificationCode = emailVerificationCodeRepository.findByUser(user)
                .orElseGet(EmailVerificationCode::new);

        LocalDateTime now = LocalDateTime.now();
        verificationCode.setUser(user);
        verificationCode.setVerificationCode(generateSixDigitCode());
        verificationCode.setLastGeneratedAt(now);
        verificationCode.setExpiresAt(now.plusMinutes(expirationMinutes));
        // On initialise createdAt uniquement lors de la première création.
        if (verificationCode.getCreatedAt() == null) {
            verificationCode.setCreatedAt(now);
        }

        emailVerificationCodeRepository.save(verificationCode);
        return verificationCode.getVerificationCode();
    }
    
//    Vérifie si le code saisi est correct et encore valide.
//    Si oui, l'utilisateur passe en emailVerified = true.
    @Transactional
    public void verifyCode(String email, String code) {
        Users user = usersRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé."));

        EmailVerificationCode verificationCode = emailVerificationCodeRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Aucun code actif trouvé."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Ce code a expiré. Demandez-en un nouveau.");
        }

        if (!verificationCode.getVerificationCode().equals(code.trim())) {
            throw new IllegalArgumentException("Code invalide.");
        }

        user.setEmailVerified(true);
        usersRepository.save(user);
        // Une fois utilisé, le code est supprimé.
        emailVerificationCodeRepository.delete(verificationCode);
    }
    
//    Retourne un utilisateur manuel à vérifier, ou null si aucun n'est trouvé.
    @Transactional(readOnly = true)
    public Users getPendingManualUserByEmail(String email) {
        return usersRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> "MANUAL".equalsIgnoreCase(user.getAuthProvider()))
                .orElse(null);
    }
    
//    Retourne la durée de validité d'un code.
    public int getExpirationMinutes() {
        return expirationMinutes;
    }
    
//    Génère un code numérique à 6 chiffres.
    private String generateSixDigitCode() {
        int value = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}