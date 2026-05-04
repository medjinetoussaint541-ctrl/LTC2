/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package com.team.apk.Controller;

/**
*
* @author HP
*/

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
public class VerifierController {

   private final UsersRepository usersRepository;
   private final ProfilService profilService;

   public VerifierController(UsersRepository usersRepository, ProfilService profilService) {
       this.usersRepository = usersRepository;
       this.profilService = profilService;
   }

   @GetMapping("/verifier")
   public String afficherVerifier(Authentication authentication, Model model) {
       Users user = loadAuthenticatedUser(authentication);
       ProfilView profil = profilService.buildProfilView(user);

       model.addAttribute("currentUser", profil.getCurrentUserView());
       return "verifier";
   }

   private Users loadAuthenticatedUser(Authentication authentication) {
       String email = resolveAuthenticatedEmail(authentication);

       if (email == null) {
           throw new IllegalStateException("Aucun utilisateur authentifié trouvé.");
       }

       return usersRepository.findByEmail(email)
               .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable."));
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
}