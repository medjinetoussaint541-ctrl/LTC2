package com.team.apk.Config;

import com.team.apk.Service.UserPresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class UserPresenceLogoutHandler implements LogoutHandler {

    private final UserPresenceService userPresenceService;

    public UserPresenceLogoutHandler(UserPresenceService userPresenceService) {
        this.userPresenceService = userPresenceService;
    }

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : null;

        if (email != null && sessionId != null) {
            userPresenceService.markOfflineIfNoOtherActiveSession(email, sessionId);
        }
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
        if (principal instanceof String principalName && !"anonymousUser".equalsIgnoreCase(principalName)) {
            return principalName.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }
}
