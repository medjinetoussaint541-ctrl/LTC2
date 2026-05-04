/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.team.apk.Config;

/**
 *
 * @author HP
 */

import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

//Gère les échecs de connexion.
// L'objectif principal est de distinguer un simple échec d'identifiants
//d'un compte manuel dont l'adresse email n'a pas encore été vérifiée.

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationFailureHandler(UsersRepository usersRepository,
                                              PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
//    Analyse la cause probable de l'échec puis redirige vers la page de connexion
//     avec un code d'erreur exploitable côté interface.
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String email = normalize(request.getParameter("email"));
        String rawPassword = request.getParameter("password");
        String errorCode = "invalid"; // Valeur par défaut : identifiants incorrects
        
        // On vérifie si l'utilisateur existe pour savoir
        // si l'échec vient d'un compte non encore vérifié.
        if (!email.isBlank()) {
            Optional<Users> optionalUser = usersRepository.findByEmail(email);
            if (optionalUser.isPresent()) {
                Users user = optionalUser.get();
                boolean isManualUser = "MANUAL".equalsIgnoreCase(user.getAuthProvider());
                boolean emailNotVerified = !Boolean.TRUE.equals(user.getEmailVerified());
                boolean passwordMatches = rawPassword != null
                        && user.getPassword() != null
                        && passwordEncoder.matches(rawPassword, user.getPassword());
                
                // Si le mot de passe est correct mais que l'email n'est pas vérifié,
                // on renvoie un code d'erreur plus précis.
                if (isManualUser && emailNotVerified && passwordMatches) {
                    errorCode = "unverified";
                }
            }
        }
        // Construction d'une URL de redirection propre avec les paramètres utiles.
        String redirectUrl = UriComponentsBuilder.fromPath("/login")
                .queryParam("error", errorCode)
                .queryParam("email", email)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
    
//    Nettoie une valeur texte pour éviter les écarts de casse et d'espaces.
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
