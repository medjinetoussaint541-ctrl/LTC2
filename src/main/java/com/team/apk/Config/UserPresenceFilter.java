package com.team.apk.Config;

import com.team.apk.Service.UserPresenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserPresenceFilter extends OncePerRequestFilter {

    private static final String PRESENCE_TOUCH_SESSION_KEY = "USER_PRESENCE_LAST_TOUCH_AT";
    private static final long TOUCH_THROTTLE_MILLIS = 30_000L;

    private final UserPresenceService userPresenceService;

    public UserPresenceFilter(UserPresenceService userPresenceService) {
        this.userPresenceService = userPresenceService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/CSS/")
                || path.startsWith("/JS/")
                || path.startsWith("/IMG/")
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = resolveAuthenticatedEmail(authentication);

        if (email != null) {
            HttpSession session = request.getSession(false);
            boolean forceTouch = "/logout".equals(request.getServletPath());

            if (forceTouch || shouldTouch(session)) {
                userPresenceService.touchOnline(email);
                if (session != null) {
                    session.setAttribute(PRESENCE_TOUCH_SESSION_KEY, System.currentTimeMillis());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldTouch(HttpSession session) {
        if (session == null) {
            return true;
        }

        Object lastTouch = session.getAttribute(PRESENCE_TOUCH_SESSION_KEY);
        if (!(lastTouch instanceof Long lastTouchMillis)) {
            return true;
        }
        return (System.currentTimeMillis() - lastTouchMillis) >= TOUCH_THROTTLE_MILLIS;
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalName && "anonymousUser".equalsIgnoreCase(principalName)) {
            return null;
        }
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
