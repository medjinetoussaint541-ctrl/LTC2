package com.team.apk.Service;

import com.team.apk.Model.StatutLine;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPresenceService {

    private static final long TOUCH_THROTTLE_SECONDS = 30L;

    private final UsersRepository usersRepository;
    private final SessionRegistry sessionRegistry;

    public UserPresenceService(UsersRepository usersRepository,
                               SessionRegistry sessionRegistry) {
        this.usersRepository = usersRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional
    public void markOnline(String email) {
        withUser(email, user -> {
            LocalDateTime now = LocalDateTime.now();
            user.setStatutLine(StatutLine.ONLINE);
            user.setLastSeen(now);
            return true;
        });
    }

    @Transactional
    public void touchOnline(String email) {
        withUser(email, user -> {
            LocalDateTime now = LocalDateTime.now();
            boolean mustRefresh = !StatutLine.ONLINE.equals(user.getStatutLine())
                    || user.getLastSeen() == null
                    || user.getLastSeen().isBefore(now.minusSeconds(TOUCH_THROTTLE_SECONDS));

            if (mustRefresh) {
                user.setStatutLine(StatutLine.ONLINE);
                user.setLastSeen(now);
            }
            return mustRefresh;
        });
    }

    @Transactional
    public void markOfflineIfNoOtherActiveSession(String email, String endingSessionId) {
        withUser(email, user -> {
            boolean hasAnotherActiveSession = findActiveSessionsForEmail(email).stream()
                    .anyMatch(session -> !session.isExpired()
                            && !Objects.equals(session.getSessionId(), endingSessionId));

            if (!hasAnotherActiveSession) {
                user.setStatutLine(StatutLine.OFFLINE);
                if (user.getLastSeen() == null) {
                    user.setLastSeen(LocalDateTime.now());
                }
                return true;
            }
            return false;
        });
    }

    private void withUser(String email, java.util.function.Function<Users, Boolean> updater) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return;
        }

        usersRepository.findByEmail(normalizedEmail)
                .ifPresent(user -> {
                    Boolean changed = updater.apply(user);
                    if (Boolean.TRUE.equals(changed)) {
                        usersRepository.save(user);
                    }
                });
    }

    private List<SessionInformation> findActiveSessionsForEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return List.of();
        }

        return sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> normalizedEmail.equals(normalizeEmail(resolveEmailFromPrincipal(principal))))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .toList();
    }

    private String resolveEmailFromPrincipal(Object principal) {
        if (principal instanceof OAuth2User oauth2User) {
            String oauthEmail = oauth2User.getAttribute("email");
            return oauthEmail == null ? null : oauthEmail.trim().toLowerCase(Locale.ROOT);
        }
        if (principal instanceof UserDetails userDetails) {
            return normalizeEmail(userDetails.getUsername());
        }
        if (principal instanceof String principalName) {
            return "anonymousUser".equalsIgnoreCase(principalName) ? null : normalizeEmail(principalName);
        }
        return null;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
