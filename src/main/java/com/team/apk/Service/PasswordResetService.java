package com.team.apk.Service;

import com.team.apk.Model.PasswordResetCode;
import com.team.apk.Model.Users;
import com.team.apk.Repository.PasswordResetCodeRepository;
import com.team.apk.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service qui gère tout le cycle de vie des codes de réinitialisation de mot de passe :
 * génération, validation et application du nouveau mot de passe.
 * Suit la même logique que VerificationCodeService
 */
@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final int expirationMinutes;

    public PasswordResetService(PasswordResetCodeRepository passwordResetCodeRepository,
                                UsersRepository usersRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.email-verification.code-expiration-minutes:10}") int expirationMinutes) {
        this.passwordResetCodeRepository = passwordResetCodeRepository;
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    /**
     * Vérifie qu'un email appartient à un compte MANUAL actif,
     * puis génère (ou renouvelle) un code de réinitialisation.
     * Retourne le code généré pour l'envoi par email.
     */
    @Transactional
    public String generateOrRefreshCode(String email) {
        Users user = usersRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé pour cet email."));

        if (!"MANUAL".equalsIgnoreCase(user.getAuthProvider())) {
            throw new IllegalStateException("Ce compte utilise la connexion Google. La réinitialisation par email n'est pas disponible.");
        }

        PasswordResetCode resetCode = passwordResetCodeRepository.findByUser(user)
                .orElseGet(PasswordResetCode::new);

        LocalDateTime now = LocalDateTime.now();
        resetCode.setUser(user);
        resetCode.setResetCode(generateSixDigitCode());
        resetCode.setLastGeneratedAt(now);
        resetCode.setExpiresAt(now.plusMinutes(expirationMinutes));
        if (resetCode.getCreatedAt() == null) {
            resetCode.setCreatedAt(now);
        }

        passwordResetCodeRepository.save(resetCode);
        return resetCode.getResetCode();
    }

    /**
     * Vérifie si le code fourni est correct et encore valide
     * sans appliquer le nouveau mot de passe (étape de validation uniquement).
     */
    @Transactional(readOnly = true)
    public void validateCode(String email, String code) {
        Users user = usersRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé."));

        PasswordResetCode resetCode = passwordResetCodeRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Aucun code actif trouvé. Veuillez faire une nouvelle demande."));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Ce code a expiré. Veuillez faire une nouvelle demande.");
        }

        if (!resetCode.getResetCode().equals(code.trim())) {
            throw new IllegalArgumentException("Code invalide.");
        }
    }

    /**
     * Valide le code puis applique le nouveau mot de passe.
     * Le code est supprimé après utilisation.
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        Users user = usersRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé."));

        PasswordResetCode resetCode = passwordResetCodeRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Aucun code actif trouvé. Veuillez faire une nouvelle demande."));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Ce code a expiré. Veuillez faire une nouvelle demande.");
        }

        if (!resetCode.getResetCode().equals(code.trim())) {
            throw new IllegalArgumentException("Code invalide.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        // Une fois utilisé, le code est supprimé.
        passwordResetCodeRepository.delete(resetCode);
    }

    /**
     * Applique le nouveau mot de passe directement.
     * À utiliser uniquement après que le code a été validé via validateCode()
     * et que l'email a été stocké en session (étape 3 du flux).
     * Le code de réinitialisation est supprimé après usage.
     */
    @Transactional
    public void applyNewPassword(String email, String newPassword) {
        Users user = usersRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte trouvé."));

        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        // Nettoyage du code en base après utilisation.
        passwordResetCodeRepository.findByUser(user)
                .ifPresent(passwordResetCodeRepository::delete);
    }

    // Retourne la durée de validité d'un code.
    public int getExpirationMinutes() {
        return expirationMinutes;
    }

    // Génère un code numérique à 6 chiffres.
    private String generateSixDigitCode() {
        int value = SECURE_RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}
