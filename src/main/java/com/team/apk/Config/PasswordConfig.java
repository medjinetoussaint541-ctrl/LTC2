package com.team.apk.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

//Configuration liée au mot de passe.
//On expose ici un bean PasswordEncoder pour hasher les mots de passe.
@Configuration
public class PasswordConfig {
    
//    Utilise BCrypt, un algorithme standard et sécurisé pour les mots de passe.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}