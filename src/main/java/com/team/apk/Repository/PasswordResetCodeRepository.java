package com.team.apk.Repository;

import com.team.apk.Model.PasswordResetCode;
import com.team.apk.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository pour gérer les codes de réinitialisation de mot de passe.
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    // Recherche le code de réinitialisation associé à un utilisateur donné.
    Optional<PasswordResetCode> findByUser(Users user);

    // Supprime le code lié à un utilisateur.
    void deleteByUser(Users user);
}
