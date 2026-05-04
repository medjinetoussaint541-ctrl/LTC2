package com.team.apk.Controller;

import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.team.apk.Model.Users;
import com.team.apk.Service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.team.apk.Service.CloudinaryService;
import com.team.apk.Service.EmailDeliveryService;
import com.team.apk.Service.VerificationCodeService;

import org.springframework.web.multipart.MultipartFile;


//Controleur principal pour l'authentification.
// Il gère :
//  - la page d'accueil publique,
//  - la connexion,
//  - l'inscription,
//  - la vérification d'email,
//  - l'accueil après connexion.

@Controller
public class AuthController {

    private final AuthService authService;
    private final CloudinaryService cloudinaryService;
    private final Environment environment;
    private final VerificationCodeService verificationCodeService;
    private final EmailDeliveryService emailDeliveryService;

    public AuthController(AuthService authService,
                      CloudinaryService cloudinaryService,
                      Environment environment,
                      VerificationCodeService verificationCodeService,
                      EmailDeliveryService emailDeliveryService) {
        this.authService = authService;
        this.cloudinaryService = cloudinaryService;
        this.environment = environment;
        this.verificationCodeService = verificationCodeService;
        this.emailDeliveryService = emailDeliveryService;
    }
    
//    Affiche la page d'accueil publique ou redirige vers le tableau de bord 
//    si l'utilisateur est déjà connecté.
    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String
                && "anonymousUser".equals(authentication.getPrincipal()))) {
            return "redirect:/accueil";
        }
        return "index";
    }
    
//    Affiche la page de connexion et prépare les messages d'erreur éventuels à afficher dans la vue.
    @GetMapping("/login")
    public String login(Authentication authentication,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String email,
                        Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String
                && "anonymousUser".equals(authentication.getPrincipal()))) {
            return "redirect:/accueil";
        }
        
//        Normalisation des données entrantes pour éviter les doublons techniques.
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        model.addAttribute("loginEmail", normalizedEmail);
        
        // On adapte le message selon la cause de l'echec de connexion
        if (error != null && !error.isBlank()) {
            switch (error.trim().toLowerCase()) {
                case "unverified" -> {
                    model.addAttribute("errorMessage",
                            "Connexion impossible : votre adresse email n'a pas encore été vérifiée.");
                    model.addAttribute("showVerifyLink", !normalizedEmail.isBlank());
                }
                case "oauth" -> model.addAttribute("errorMessage",
                        "La connexion avec Google a échoué. Veuillez réessayer.");
                case "expired" -> model.addAttribute("errorMessage",
                        "Votre session a expiré. Veuillez vous reconnecter.");
                default -> model.addAttribute("errorMessage",
                        "Email ou mot de passe incorrect.");
            }
        }

        model.addAttribute("googleAuthEnabled", isGoogleAuthEnabled());
        return "login";
    }
    
//    Affiche le formulaire d'inscription avec des valeurs vides par défaut.
    @GetMapping("/register")
    public String registerPage(Model model) {
        fillDefaultFormValues(model, "", "", "", "", "");
        model.addAttribute("googleAuthEnabled", isGoogleAuthEnabled());
        return "register";
    }

    
//    Traite l'inscription d'un utilisateur manuel.
//     Étapes :
//      1. normaliser les données,
//      2. valider les champs,
//      3. téléverser éventuellement la photo,
//      4. créer le compte,
//      5. générer et envoyer le code de vérification.
    
    @PostMapping("/register")
    public String register(@RequestParam String prenom,
                           @RequestParam String nom,
                           @RequestParam String sexe,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(required = false, name = "photo") MultipartFile photo,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
//        Normalisation des données entrantes pour éviter les doublons techniques.
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedPrenom = prenom == null ? "" : prenom.trim();
        String normalizedNom = nom == null ? "" : nom.trim();
        String normalizedSexe = sexe == null ? "" : sexe.trim().toUpperCase();
        
        // On réinjecte les valeurs dans le formulaire en cas d'erreur
        
        fillDefaultFormValues(model, normalizedPrenom, normalizedNom, normalizedSexe, normalizedEmail, "");
        model.addAttribute("googleAuthEnabled", isGoogleAuthEnabled());

        if (normalizedPrenom.isBlank() || normalizedNom.isBlank() || normalizedEmail.isBlank() || password.isBlank()) {
            model.addAttribute("errorMessage", "Tous les champs obligatoires doivent être renseignés.");
            return "register";
        }

        if (!normalizedSexe.equals("M") && !normalizedSexe.equals("F")) {
            model.addAttribute("errorMessage", "Le sexe doit être M ou F pour respecter la structure actuelle.");
            return "register";
        }

        if (password.length() < 8) {
            model.addAttribute("errorMessage", "Le mot de passe doit contenir au moins 8 caractères.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Les mots de passe ne correspondent pas.");
            return "register";
        }

        if (authService.emailExists(normalizedEmail)) {
            model.addAttribute("errorMessage", "Cet email existe déjà.");
            return "register";
        }

        try {
//            Upload optionnel de la photo avant création du compte.
            String uploadedPhotoUrl = cloudinaryService.uploadProfileImage(photo);
//            Création du compte et de son profil associé
            Users createdUser = authService.registerUser(
                    normalizedPrenom,
                    normalizedNom,
                    normalizedSexe,
                    normalizedEmail,
                    password,
                    uploadedPhotoUrl
            );
            
            // Après création du compte, on génère un code à usage temporaire.
            String verificationCode = verificationCodeService.generateOrRefreshCode(createdUser);
            try {
                emailDeliveryService.sendVerificationCode(normalizedEmail, verificationCode);
                redirectAttributes.addFlashAttribute("successMessage", "Compte créé. Un code de vérification a été envoyé à votre adresse email.");
            } catch (IllegalStateException e) {
//                compte créé mais SMTP non configuré
                redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
            }

            redirectAttributes.addAttribute("email", normalizedEmail);
            return "redirect:/verify-email";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Erreur technique : " + e.getMessage());
            return "register";
        }
    }

//    Affiche la page où l'utilisateur saisit son code de vérification.
    @GetMapping("/verify-email")
    public String verifyEmailPage(@RequestParam(required = false) String email, Model model) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        model.addAttribute("email", normalizedEmail);
        model.addAttribute("expirationMinutes", verificationCodeService.getExpirationMinutes());
        return "verify-email";
    }
    
//    Valide le code saisi puis active le compte si la vérification réussit
    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam String email,
                            @RequestParam String code,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        String normalizedEmail = email.trim().toLowerCase();

        try {
            verificationCodeService.verifyCode(normalizedEmail, code);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Email vérifié avec succès. Vous pouvez maintenant vous connecter.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("email", normalizedEmail);
            model.addAttribute("expirationMinutes", verificationCodeService.getExpirationMinutes());
            model.addAttribute("errorMessage", e.getMessage());
            return "verify-email";
        }
    }
    
//    Génère puis renvoie un nouveau code de vérification.
//    L'ancien code devient automatiquement invalide.
    @PostMapping("/verify-email/resend")
    public String resendVerificationCode(@RequestParam String email,
                                        RedirectAttributes redirectAttributes) {
        String normalizedEmail = email.trim().toLowerCase();

        try {
            Users pendingUser = verificationCodeService.getPendingManualUserByEmail(normalizedEmail);

            if (pendingUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Aucun compte manuel en attente n'a été trouvé pour cet email.");
            } else if (Boolean.TRUE.equals(pendingUser.getEmailVerified())) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Cet email est déjà vérifié. Vous pouvez vous connecter.");
                return "redirect:/login";
            } else {
                String newCode = verificationCodeService.generateOrRefreshCode(pendingUser);
                emailDeliveryService.sendVerificationCode(normalizedEmail, newCode);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Un nouveau code a été envoyé. L'ancien code n'est plus valide.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
        }

        redirectAttributes.addAttribute("email", normalizedEmail);
        return "redirect:/verify-email";
    }

    
//    Remplit les valeurs du formulaire côté vue.
//    Cela évite de perdre les données déjà saisies après une erreur.
    private void fillDefaultFormValues(Model model,
                                       String prenom,
                                       String nom,
                                       String sexe,
                                       String email,
                                       String photoUrl) {
        model.addAttribute("prenom", prenom);
        model.addAttribute("nom", nom);
        model.addAttribute("sexe", sexe);
        model.addAttribute("email", email);
        model.addAttribute("photoUrl", photoUrl);
    }

    
//    Récupère l'email du compte actuellement connecté,
//    que ce soit via formulaire classique ou via Google.
    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        
        // Cas d'une authentification OAuth2.
        
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            if (oauthEmail != null && !oauthEmail.isBlank()) {
                return oauthEmail.trim().toLowerCase();
            }
        }
        
        
        // Cas d'une authentification classique Spring Security
        String name = authentication.getName();
        return name == null ? null : name.trim().toLowerCase();
    }
    
//    Vérifie si la configuration Google OAuth est présente.
//    Cela permet d'afficher ou masquer le bouton Google côté interface.
    private boolean isGoogleAuthEnabled() {
        String clientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id");
        String clientSecret = environment.getProperty("spring.security.oauth2.client.registration.google.client-secret");
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
