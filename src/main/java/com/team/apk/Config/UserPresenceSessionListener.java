package com.team.apk.Config;

import com.team.apk.Service.UserPresenceService;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class UserPresenceSessionListener implements ApplicationListener<SessionDestroyedEvent> {

    private final UserPresenceService userPresenceService;

    public UserPresenceSessionListener(UserPresenceService userPresenceService) {
        this.userPresenceService = userPresenceService;
    }

    @Override
    public void onApplicationEvent(SessionDestroyedEvent event) {
        Set<String> emails = new LinkedHashSet<>();
        for (SecurityContext securityContext : event.getSecurityContexts()) {
            String email = resolveAuthenticatedEmail(securityContext.getAuthentication());
            if (email != null) {
                emails.add(email);
            }
        }

        for (String email : emails) {
            userPresenceService.markOfflineIfNoOtherActiveSession(email, event.getId());
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
