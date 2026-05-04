package com.team.apk.Service;

import com.team.apk.Model.Persons;
import com.team.apk.Model.Users;
import com.team.apk.Repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

//Service métier lié à l'authentification et à la création des comptes.
@Service
public class AuthService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsersRepository usersRepository, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Vérifie si un email existe déjà dans la base de donnee.
    public boolean emailExists(String email) {
        return usersRepository.countByEmailNative(email) > 0;
    }

    // Crée un nouvel utilisateur inscrit manuellement.
    @Transactional
    public Users registerUser(String prenom,
            String nom,
            String sexe,
            String email,
            String password,
            String photoUrl) {

        Users user = new Users();
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(password)); // Le mot de passe est stocké hashé.
        user.setAuthProvider("MANUAL");
        user.setRole("USER");
        user.setEmailVerified(false);

        Persons person = new Persons();
        person.setPrenom(prenom == null ? null : prenom.trim());
        person.setNom(nom == null ? null : nom.trim());
        person.setSexe(sexe == null ? null : sexe.trim().toUpperCase());
        person.setPhotoUrl((photoUrl == null || photoUrl.isBlank()) ? null : photoUrl.trim());

        // On relie les deux entités dans les deux sens.
        person.setUser(user);
        user.setPerson(person);

        return usersRepository.save(user);
    }

    @Transactional
    public void registerOrUpdateGoogleUser(String prenom,
            String nom,
            String email,
            String photoUrl) {

        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("L'email Google est obligatoire.");
        }

        Optional<Users> existingUserOptional = usersRepository.findByEmail(normalizedEmail);
        Users user = existingUserOptional.orElseGet(Users::new);

        boolean isNewUser = (user.getUserId() == null);

        if (isNewUser) {
            user.setEmail(normalizedEmail);
            user.setPassword(null);
            user.setAuthProvider("GOOGLE");
            user.setRole("USER");
            user.setEmailVerified(true);
        }

        Persons person = user.getPerson();
        if (person == null) {
            person = new Persons();
            person.setUser(user);
            user.setPerson(person);
        }

        /*
         * 
         * Mise à jour du prénom et nom et du photo uniquement s'ils sont vides (ou pour
         * un nouvel
         * utilisateur) dans le but d'eviter d'ecraser les modifications des photos
         * profil, de reprendre la photo de google si elle n'existe pas deja et de ne
         * pas ecraser les prenom et nom et photo deja existant a chaque connextion
         * 
         */
        if (isNewUser || person.getPrenom() == null || person.getPrenom().isBlank()) {
            if (prenom != null && !prenom.isBlank()) {
                person.setPrenom(prenom.trim());
            }
        }
        if (isNewUser || person.getNom() == null || person.getNom().isBlank()) {
            if (nom != null && !nom.isBlank()) {
                person.setNom(nom.trim());
            }
        }

        if (isNewUser || (photoUrl != null && !photoUrl.isBlank() && person.getPhotoUrl() == null)) {
            person.setPhotoUrl(photoUrl.trim());
        }

        usersRepository.save(user);
    }
}
