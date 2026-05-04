package com.team.apk.Controller;

import com.team.apk.Dto.LiensDashboardView;
import com.team.apk.Model.Users;
import com.team.apk.Model.Persons;
import com.team.apk.Repository.UsersRepository;
import com.team.apk.Service.ProfilService;
import com.team.apk.Service.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/*C ontroleur pour la page (Moi)
    Il gere l'affiche  des informations de l'utilisateur, sa relation actuelle,
    et les actions de modification de photo profil.
*/

@Controller
public class ProfilController {

    private final ProfilService profilService;
    private final UsersRepository usersRepository;
    private final CloudinaryService cloudinaryService;

    public ProfilController(ProfilService profilService,
            UsersRepository usersRepository, CloudinaryService cloudinaryService) {
        this.profilService = profilService;
        this.usersRepository = usersRepository;
        this.cloudinaryService = cloudinaryService;

    }

    @GetMapping("/profil")
    public String profil(Authentication authentication, Model model) {
        Users user = loadAuthenticatedUser(authentication);
        model.addAttribute("profil", profilService.buildProfilView(user));
        return "profil";
    }

    @PostMapping("/profil/photo")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> updateProfilePhoto(
            @RequestParam("photo") MultipartFile photo,
            Authentication authentication) {

        Users user = loadAuthenticatedUser(authentication);
        Persons person = user.getPerson();

        if (person == null) {
            person = new Persons();
            person.setUser(user);
            user.setPerson(person);
        }

        try {
            // Upload vers Cloudinary
            String photoUrl = cloudinaryService.uploadProfileImage(photo);
            person.setPhotoUrl(photoUrl);
            usersRepository.save(user);
            return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/profil/visibilite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateProfileVisibility(
            @RequestParam("visible") boolean visible,
            Authentication authentication) {

        Users user = loadAuthenticatedUser(authentication);
        boolean profileVisible = profilService.updateProfileVisibility(user, visible);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "visible", profileVisible,
                "message", profileVisible
                        ? "Votre profil est désormais visible dans la section des utilisateurs en ligne."
                        : "Votre profil est désormais masqué dans la section des utilisateurs en ligne."
        ));
    }

    // Recuperation de l'email de l'utilisateur connecte
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

    // Chargement de l'utilisateur authentifie
    private Users loadAuthenticatedUser(Authentication authentication) {

        String email = resolveAuthenticatedEmail(authentication);

        if (email == null) {

            throw new IllegalStateException("Utilisateur non authentifie ");

        }

        return usersRepository.findByEmail(email)
                .orElseThrow(
                        () -> new IllegalStateException("Utilisateur non trouve dans la base de donnees : " + email));

    }

}
