package com.team.apk.Controller;

import com.team.apk.Dto.AccueilDashboardView;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import com.team.apk.Service.AccueilService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

//Contrôleur de l'écran d'accueil

@Controller
public class AccueilController {

    private final UsersRepository usersRepository;
    private final AccueilService accueilService;

    public AccueilController(UsersRepository usersRepository,
                             AccueilService accueilService) {
        this.usersRepository = usersRepository;
        this.accueilService = accueilService;
    }
    
//    Affiche la page d'accueil avec l'ensemble des donnees de synthese.
    @GetMapping("/accueil")
    public String accueil(Authentication authentication, Model model) {
        Users user = loadAuthenticatedUser(authentication);
        AccueilDashboardView dashboard = accueilService.buildDashboard(user);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("currentUser", dashboard.getCurrentUser());
        model.addAttribute("stats", dashboard.getStats());
        model.addAttribute("pendingRequests", dashboard.getPendingRequests());
        model.addAttribute("activityItems", dashboard.getActivityItems());
        model.addAttribute("activeRelation", dashboard.getActiveRelation());
        return "accueil";
    }
    
    //Affiche la page complète de l historique des activités
    @GetMapping("/historique")
    public String historique(Authentication authentication, Model model) {
        Users user = loadAuthenticatedUser(authentication);
        AccueilDashboardView dashboard = accueilService.buildDashboard(user);

        model.addAttribute("currentUser", dashboard.getCurrentUser());
        model.addAttribute("stats", dashboard.getStats());
        model.addAttribute("activityItems", accueilService.buildFullActivityHistory(user));
        return "historique";
    }

//    Accepte une demande reçue depuis l'écran d'accueil.
    @PostMapping("/demandes/{id}/accepter")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> acceptRequest(@PathVariable("id") Long demandeId,
                                                             Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(accueilService.acceptPendingRequest(currentUser.getUserId(), demandeId));
    }
    
//    Refuse une demande reçue depuis l'écran d'accueil.
    @PostMapping("/demandes/{id}/refuser")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> declineRequest(@PathVariable("id") Long demandeId,
                                                              Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(accueilService.declinePendingRequest(currentUser.getUserId(), demandeId));
    }
    
//    Charge depuis la base de donnee l'utilisateur correspondant à l'authentification courante.
    private Users loadAuthenticatedUser(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        if (email == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié n'a été trouvé.");
        }
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable en base."));
    }
    
//    Extrait l'email de l'utilisateur connecté
    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null) {
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
