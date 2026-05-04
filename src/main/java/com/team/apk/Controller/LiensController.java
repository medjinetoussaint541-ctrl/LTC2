package com.team.apk.Controller;

import com.team.apk.Dto.LiensDashboardView;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import com.team.apk.Service.LiensService;
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

//Contrôleur de la page 'Mes liens'. 
//Il pilote l'affichage des relations, demandes et crushs, ainsi que les actions de 
//modification associées.

@Controller
public class LiensController {

    private final UsersRepository usersRepository;
    private final LiensService liensService;

    public LiensController(UsersRepository usersRepository,
            LiensService liensService) {
        this.usersRepository = usersRepository;
        this.liensService = liensService;
    }

    // Affiche la page récapitulative des liens de l'utilisateur connecté
    @GetMapping("/liens")
    public String liens(Authentication authentication, Model model) {
        Users user = loadAuthenticatedUser(authentication);
        LiensDashboardView dashboard = liensService.buildDashboard(user);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("currentUser", dashboard.getCurrentUser());
        model.addAttribute("activeRelation", dashboard.getActiveRelation());
        model.addAttribute("exRelations", dashboard.getExRelations());
        model.addAttribute("demandesEnvoyees", dashboard.getDemandesEnvoyees());
        model.addAttribute("demandesRecues", dashboard.getDemandesRecues());
        model.addAttribute("crushesAjoutes", dashboard.getCrushesAjoutes());
        return "mes-liens";
    }

    // Annule une demande envoyée encore en attente
    @PostMapping("/liens/demandes/{id}/annuler")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelSentRequest(@PathVariable("id") Long demandeId,
            Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(liensService.cancelSentPendingRequest(currentUser.getUserId(), demandeId));
    }

    // Changer un crush vers le statut d'ex-crush
    @PostMapping("/liens/crushs/{id}/ex-crush")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markCrushAsExCrush(@PathVariable("id") Long crushId,
            Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(liensService.markCrushAsExCrush(currentUser.getUserId(), crushId));
    }

    // Met fin à une relation active
    @PostMapping("/liens/relations/{id}/rompre")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> breakRelation(@PathVariable("id") Long relationId,
            Authentication authentication) {
        Users currentUser = loadAuthenticatedUser(authentication);
        return ResponseEntity.ok(liensService.breakActiveRelation(currentUser.getUserId(), relationId));
    }

    // Charge l'utilisateur authentifié
    private Users loadAuthenticatedUser(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        if (email == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié trouvé.");
        }
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable en base."));
    }

    // Récupère l'email de l'utilisateur connecté
    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null)
            return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null && !oauthEmail.isBlank())
                return oauthEmail.trim().toLowerCase();
        }
        String name = authentication.getName();
        return name == null ? null : name.trim().toLowerCase();
    }
}
