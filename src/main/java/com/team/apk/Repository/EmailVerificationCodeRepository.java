/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team.apk.Repository;

/**
 *
 * @author HP
 */

import com.team.apk.Model.EmailVerificationCode;
import com.team.apk.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//Repository pour gérer les codes de vérification d'email.
public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
//    Recherche le code de vérification associé à un utilisateur donné.
    Optional<EmailVerificationCode> findByUser(Users user);
    
//    Supprime le code lié à un utilisateur.
    void deleteByUser(Users user);
}