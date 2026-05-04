package com.team.apk.Controller;

import com.team.apk.Dto.ProfilView;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import com.team.apk.Service.ProfilService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SymbioseController {

    private final ProfilService profilService;
    private final UsersRepository usersRepository;

    public SymbioseController(ProfilService profilService, UsersRepository usersRepository) {
        this.profilService = profilService;
        this.usersRepository = usersRepository;
    }

    @GetMapping("/symbiose")
    public String afficherSymbiose(Authentication authentication, Model model) {
        Users currentUser = loadAuthenticatedUser(authentication);
        ProfilView profil = profilService.buildProfilView(currentUser);
        model.addAttribute("profil", profil);
        return "symbiose";
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null && !oauthEmail.isBlank()) {
                return oauthEmail.trim().toLowerCase();
            }
        }
        String name = authentication.getName();
        return name == null ? null : name.trim().toLowerCase();
    }

    private Users loadAuthenticatedUser(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        if (email == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé : " + email));
    }
}