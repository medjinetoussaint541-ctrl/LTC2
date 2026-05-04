package com.team.apk.Controller;

import com.team.apk.Service.EmailDeliveryService;
import com.team.apk.Service.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controleur pour la réinitialisation du mot de passe en 3 étapes séparées.
 *
 *  Etape 1 — GET  /forgot-password        → formulaire saisie email
 *             POST /forgot-password        → envoi du code, redirect /reset-password
 *
 *  Etape 2 — GET  /reset-password         → formulaire saisie du code uniquement
 *             POST /reset-password         → validation du code
 *                                            → stocke l'email en session (resetVerifiedEmail)
 *                                            → redirect /new-password
 *             POST /reset-password/resend → renvoi d'un nouveau code
 *
 *  Etape 3 — GET  /new-password           → formulaire nouveau mot de passe
 *                                            (bloqué si session resetVerifiedEmail absente)
 *             POST /new-password          → application du nouveau mot de passe
 *                                            → supprime la session, redirect /login
 */
@Controller
public class PasswordResetController {

    private final String sessionKey;
    private final PasswordResetService passwordResetService;
    private final EmailDeliveryService emailDeliveryService;

    public PasswordResetController(PasswordResetService passwordResetService,
                                   EmailDeliveryService emailDeliveryService,
                                   @Value("${app.password-reset.session-key}") String sessionKey) {
        this.passwordResetService = passwordResetService;
        this.emailDeliveryService = emailDeliveryService;
        this.sessionKey = sessionKey;
    }

    // ───────────────────────────────────────────
    // Etape 1 : Saisie de l'email
    // ───────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 RedirectAttributes redirectAttributes) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        try {
            String resetCode = passwordResetService.generateOrRefreshCode(normalizedEmail);
            try {
                emailDeliveryService.sendPasswordResetCode(normalizedEmail, resetCode);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Un code de réinitialisation a été envoyé à votre adresse email.");
            } catch (IllegalStateException e) {
                redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
            }
            redirectAttributes.addAttribute("email", normalizedEmail);
            return "redirect:/reset-password";

        } catch (IllegalStateException e) {
            // Compte Google : on affiche le message directement sur forgot-password.
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/forgot-password";

        } catch (IllegalArgumentException e) {
            // Email inexistant : message neutre pour ne pas révéler si l'email existe.
            redirectAttributes.addFlashAttribute("successMessage",
                    "Si cet email est associé à un compte, vous recevrez un code sous peu.");
            redirectAttributes.addAttribute("email", normalizedEmail);
            return "redirect:/reset-password";
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Etape 2 : Saisie du code de vérification uniquement
    // ────────────────────────────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String email, Model model) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        model.addAttribute("email", normalizedEmail);
        model.addAttribute("expirationMinutes", passwordResetService.getExpirationMinutes());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String verifyResetCode(@RequestParam String email,
                                  @RequestParam String code,
                                  Model model,
                                  HttpSession session) {
        String normalizedEmail = email.trim().toLowerCase();

        try {
            // Valide le code sans toucher au mot de passe.
            passwordResetService.validateCode(normalizedEmail, code);

            // Code valide : on mémorise l'email en session pour autoriser l'étape 3.
            session.setAttribute(sessionKey, normalizedEmail);

            return "redirect:/new-password";

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("email", normalizedEmail);
            model.addAttribute("expirationMinutes", passwordResetService.getExpirationMinutes());
            model.addAttribute("errorMessage", e.getMessage());
            return "reset-password";
        }
    }

    // Renvoi d'un nouveau code depuis l'étape 2.
    @PostMapping("/reset-password/resend")
    public String resendResetCode(@RequestParam String email,
                                  RedirectAttributes redirectAttributes) {
        String normalizedEmail = email.trim().toLowerCase();

        try {
            String newCode = passwordResetService.generateOrRefreshCode(normalizedEmail);
            emailDeliveryService.sendPasswordResetCode(normalizedEmail, newCode);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Un nouveau code a été envoyé. L'ancien code n'est plus valide.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("warningMessage", e.getMessage());
        }

        redirectAttributes.addAttribute("email", normalizedEmail);
        return "redirect:/reset-password";
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Etape 3 : Saisie du nouveau mot de passe
    // Accessible uniquement si la session contient resetVerifiedEmail.
    // ──────────────────────────────────────────────────────────────────────────────

    @GetMapping("/new-password")
    public String newPasswordPage(HttpSession session, Model model) {
        String verifiedEmail = (String) session.getAttribute(sessionKey);

        // Accès direct sans avoir validé le code → retour étape 1.
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", verifiedEmail);
        return "new-password";
    }

    @PostMapping("/new-password")
    public String applyNewPassword(@RequestParam String newPassword,
                                   @RequestParam String confirmPassword,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String verifiedEmail = (String) session.getAttribute(sessionKey);

        // Accès direct sans session → retour étape 1.
        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            return "redirect:/forgot-password";
        }

        model.addAttribute("email", verifiedEmail);

        if (newPassword.length() < 8) {
            model.addAttribute("errorMessage", "Le mot de passe doit contenir au moins 8 caractères.");
            return "new-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Les mots de passe ne correspondent pas.");
            return "new-password";
        }

        try {
            passwordResetService.applyNewPassword(verifiedEmail, newPassword);

            // Session nettoyée après usage.
            session.removeAttribute(sessionKey);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Mot de passe réinitialisé avec succès. Vous pouvez maintenant vous connecter.");
            return "redirect:/login";

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "new-password";
        }
    }
}
