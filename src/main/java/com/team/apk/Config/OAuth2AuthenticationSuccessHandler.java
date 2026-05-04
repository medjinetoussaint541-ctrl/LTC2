package com.team.apk.Config;

import com.team.apk.Service.AuthService;
import com.team.apk.Service.UserPresenceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//Gère la connexion réussie via OAuth2 (Google).
//Après authentification, on crée l'utilisateur s'il n'existe pas encore
//ou on met à jour certaines informations utiles.
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final UserPresenceService userPresenceService;

    public OAuth2AuthenticationSuccessHandler(AuthService authService,
                                              UserPresenceService userPresenceService) {
        this.authService = authService;
        this.userPresenceService = userPresenceService;
    }
    
//    Récupère les informations envoyées par Google puis redirige vers l'accueil
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        
        // Sans email, on ne peut pas rattacher correctement le compte
        if (email == null || email.isBlank()) {
            response.sendRedirect("/login?error");
            return;
        }

        authService.registerOrUpdateGoogleUser(
                oauth2User.getAttribute("given_name"),
                oauth2User.getAttribute("family_name"),
                email,
                oauth2User.getAttribute("picture")
        );
        userPresenceService.markOnline(email);

        response.sendRedirect("/accueil");
    }
}
