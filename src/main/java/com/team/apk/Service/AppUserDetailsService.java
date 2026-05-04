package com.team.apk.Service;

import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

//Service utilisé par Spring Security pour charger les informations
//d'un utilisateur à partir de son email.
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    public AppUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }
    
//    Charge l'utilisateur à partir de l'email saisi sur le formulaire de connexion.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        Users user = usersRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Aucun utilisateur trouvé avec cet email."));
        
        // Un compte manuel non vérifié est considéré comme désactivé
        // pour empêcher la connexion avant validation de l'email.
        boolean disabled = "MANUAL".equalsIgnoreCase(user.getAuthProvider())
                && !Boolean.TRUE.equals(user.getEmailVerified());

        return User.withUsername(user.getEmail())
                .password(user.getPassword() == null ? "" : user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(disabled)
                .build();
    }
}
