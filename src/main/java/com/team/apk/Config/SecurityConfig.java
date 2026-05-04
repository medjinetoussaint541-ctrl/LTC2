package com.team.apk.Config;

import com.team.apk.Service.CustomOAuth2UserService;
import com.team.apk.Service.AppUserDetailsService;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;


//Configuration globale de Spring Security.
// Cette classe définit :
//  - les routes publiques et protégées,
//  - la connexion classique par formulaire,
//  - la connexion Google,
//  - la déconnexion.

@Configuration
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final Environment environment;

    public SecurityConfig(AppUserDetailsService userDetailsService,
                      CustomOAuth2UserService customOAuth2UserService,
                      CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
                      Environment environment) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.environment = environment;
    }
    
//    Declare la chaine de filtres de sécurité utilisée par l'application.
//    Cette méthode définit les règles d'accès, les pages de login, la gestion
//    des erreurs d'authentification et la connexion OAuth2.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                                UserPresenceAuthenticationSuccessHandler userPresenceAuthenticationSuccessHandler,
                                                UserPresenceFilter userPresenceFilter,
                                                UserPresenceLogoutHandler userPresenceLogoutHandler,
                                                SessionRegistry sessionRegistry) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Ces routes restent accessibles sans connexion
                        .requestMatchers("/", "/login", "/register", "/verify-email", "/verify-email/**", 
                                 "/forgot-password", "/reset-password", "/reset-password/**", "/new-password",
                                 "/CSS/**", "/JS/**", "/IMG/**", "/error").permitAll()
                        // Tout le reste nécessite un utilisateur authentifié
                        .anyRequest().authenticated())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login?expired")
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry)
                )        
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler(userPresenceAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(userPresenceLogoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));

        if (isGoogleOauthConfigured()) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureUrl("/login?error=oauth"));
        }

        http.addFilterAfter(userPresenceFilter, SecurityContextHolderFilter.class);

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }
    
    
//    Fournit à Spring Security la logique de chargement d'utilisateur
//    et l'encodeur de mot de passe à utiliser pour la connexion manuelle.
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    private boolean isGoogleOauthConfigured() {
        String clientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id", "");
        String clientSecret = environment.getProperty("spring.security.oauth2.client.registration.google.client-secret", "");
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
