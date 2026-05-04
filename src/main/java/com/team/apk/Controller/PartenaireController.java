package com.team.apk.Controller;

import com.team.apk.Dto.PartenaireSearchResultView;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import com.team.apk.Service.PartenaireService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

//Contrôleur de recherche et d'ajout de partenaire. 
//Il sert l'interface de recherche puis relaie les actions métier de création de demande 
//ou d'ajout en crush
@Controller
@RequestMapping("/partenaire")
public class PartenaireController {

    private final UsersRepository usersRepository;
    private final PartenaireService partenaireService;

    public PartenaireController(UsersRepository usersRepository,
                                PartenaireService partenaireService) {
        this.usersRepository = usersRepository;
        this.partenaireService = partenaireService;
    }
    
//    Affiche la page d'ajout de partenaire et prépare les informations du compte courant
    @GetMapping("/ajouter")
    public String ajouterPage(Authentication authentication, Model model) {
        Users user = loadAuthenticatedUser(authentication);
        model.addAttribute("currentUser", buildCurrentUserModel(user));
        return "partenaire-ajouter";
    }

//    Recherche des utilisateurs
    @GetMapping("/rechercher")
    @ResponseBody
    public ResponseEntity<List<PartenaireSearchResultView>> rechercher(
            @RequestParam(name = "q", defaultValue = "") String query,
            Authentication authentication) {

        Users currentUser = loadAuthenticatedUser(authentication);
        List<PartenaireSearchResultView> results = partenaireService.search(currentUser, query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/en-ligne")
    @ResponseBody
    public ResponseEntity<List<PartenaireSearchResultView>> listerUtilisateursEnLigne(
            Authentication authentication) {

        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(partenaireService.listOnlineUsers(currentUser));
    }
    
//    Déclenche l'envoi d'une demande de relation vers l'utilisateur ciblé
    @PostMapping("/demande/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> envoyerDemande(
            @PathVariable("id") Long receveurId,
            @RequestParam(name = "confirmer", defaultValue = "false") boolean confirmer,
            Authentication authentication) {

        Users currentUser = loadAuthenticatedUser(authentication);
        Map<String, Object> result = partenaireService.envoyerDemande(currentUser, receveurId, confirmer);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        if (Boolean.TRUE.equals(result.get("requiresConfirmation"))) {
            return ResponseEntity.status(409).body(result);
        }

        String code = String.valueOf(result.getOrDefault("code", ""));
        int status = switch (code) {
            case "SELF_REQUEST_NOT_ALLOWED" -> 400;
            case "USER_NOT_FOUND" -> 404;
            case "PENDING_REQUEST_ALREADY_EXISTS", "ALREADY_IN_RELATION_WITH_YOU" -> 409;
            default -> 400;
        };
        return ResponseEntity.status(status).body(result);
    }
    
//    Ajoute un utilisateur dans la liste des crushs du compte courant
    @PostMapping("/crush/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ajouterCrush(@PathVariable("id") Long targetId,
                                                            Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        Map<String, Object> result = partenaireService.ajouterCrush(currentUser, targetId);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }

        String code = String.valueOf(result.getOrDefault("code", ""));
        int status = switch (code) {
            case "SELF_REQUEST_NOT_ALLOWED" -> 400;
            case "USER_NOT_FOUND" -> 404;
            case "CRUSH_ALREADY_ADDED", "ALREADY_IN_RELATION_WITH_YOU" -> 409;
            default -> 400;
        };
        return ResponseEntity.status(status).body(result);
    }

//    Charge l'utilisateur correspondant à la session courante
    private Users loadAuthenticatedUser(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        if (email == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié trouvé.");
        }
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable en base."));
    }

//    Extrait l'email technique de l'utilisateur connecté
    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null && !oauthEmail.isBlank()) return oauthEmail.trim().toLowerCase();
        }
        String name = authentication.getName();
        return name == null ? null : name.trim().toLowerCase();
    }

    private Map<String, Object> buildCurrentUserModel(Users user) {
        var person = user.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        String initials = computeInitials(prenom, nom);

        return Map.of(
                "userId", user.getUserId(),
                "prenom", prenom,
                "fullName", fullName.isBlank() ? user.getEmail() : fullName,
                "email", user.getEmail(),
                "photoUrl", photoUrl != null ? photoUrl : "",
                "initials", initials
        );
    }
    
//    Construit les initiales à partir du prénom et du nom
    private String computeInitials(String firstName, String lastName) {
        String f = blankToNull(firstName);
        String l = blankToNull(lastName);
        StringBuilder sb = new StringBuilder();
        if (f != null) sb.append(Character.toUpperCase(f.charAt(0)));
        if (l != null) sb.append(Character.toUpperCase(l.charAt(0)));
        return sb.isEmpty() ? "U" : sb.toString();
    }

    private String normalize(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        return v.isBlank() ? fallback : v;
    }

    private String blankToNull(String value) {
        String v = value == null ? null : value.trim();
        return (v == null || v.isBlank()) ? null : v;
    }
}
