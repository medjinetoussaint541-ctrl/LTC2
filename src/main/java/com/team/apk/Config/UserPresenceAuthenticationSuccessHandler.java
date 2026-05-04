package com.team.apk.Config;

import com.team.apk.Service.UserPresenceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class UserPresenceAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserPresenceService userPresenceService;

    public UserPresenceAuthenticationSuccessHandler(UserPresenceService userPresenceService) {
        this.userPresenceService = userPresenceService;
        setDefaultTargetUrl("/accueil");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = resolveAuthenticatedEmail(authentication);
        if (email != null) {
            userPresenceService.markOnline(email);
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            return oauthEmail == null ? null : oauthEmail.trim().toLowerCase(Locale.ROOT);
        }
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
        }
        String name = authentication.getName();
        return name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }
}
