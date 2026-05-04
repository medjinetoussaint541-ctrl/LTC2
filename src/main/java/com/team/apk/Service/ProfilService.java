package com.team.apk.Service;

import com.team.apk.Dto.ActiveRelationView;
import com.team.apk.Dto.CurrentUserView;
import com.team.apk.Dto.LiensDashboardView;
import com.team.apk.Dto.ProfilView;
import com.team.apk.Model.Relations;
import com.team.apk.Model.Users;
import com.team.apk.Model.Persons;
import com.team.apk.Model.Visibilite;
import com.team.apk.Repository.RelationsRepository;
import com.team.apk.Repository.UsersRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.team.apk.Service.LiensService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class ProfilService {

    private final UsersRepository usersRepository;
    private final RelationsRepository relationsRepository;
    private final LiensService liensService;

    public ProfilService(UsersRepository usersRepository,
            RelationsRepository relationsRepository,
            LiensService liensService) {

        this.usersRepository = usersRepository;
        this.relationsRepository = relationsRepository;
        this.liensService = liensService;

    }

    @Transactional
    public ProfilView buildProfilView(Users users) {

        // Recuperation du dashboard des liens(pour exporter la relation active)
        LiensDashboardView dashboard = liensService.buildDashboard(users);
        ActiveRelationView relationActive = dashboard.getActiveRelation();

        // Conversion utilisateur en CurrentUserView
        CurrentUserView currentUser = toCurrentUserView(users);

        return new ProfilView(currentUser, relationActive, currentUser.getSexe());
    }

    @Transactional
    public boolean updateProfileVisibility(Users user, boolean visible) {
        user.setVisibilite(visible ? Visibilite.ON : Visibilite.OFF);
        usersRepository.save(user);
        return Visibilite.ON.equals(user.getVisibilite());
    }

    // Methode utilitaire pour convertir User en CurrentUserView
    private CurrentUserView toCurrentUserView(Users users) {

        Persons person = users.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");

        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        boolean verified = users.getUserverified();
        String sexe = person != null ? person.getSexe() : null;
        boolean profileVisible = users.getVisibilite() != null && users.getVisibilite() == com.team.apk.Model.Visibilite.ON;

        return new CurrentUserView(
                users.getUserId(),
                prenom,
                nom,
                fullName.isBlank() ? users.getEmail() : fullName,
                users.getEmail(),
                photoUrl,
                computeIntials(prenom, nom),
                sexe,
                verified,
                profileVisible
        );

    }

    // Methodde utilitaire que j'ai pris depuis LiensService
    private String computeIntials(String firstName, String lastName) {

        String first = blankToNull(firstName);
        String last = blankToNull(lastName);
        StringBuilder sb = new StringBuilder();

        // Changement de la premiere lettre du nom et prenom en majuscule
        if (first != null)
            sb.append(Character.toUpperCase(first.charAt(0)));
        if (last != null)
            sb.append(Character.toUpperCase(last.charAt(0)));

        return sb.isEmpty() ? "U" : sb.toString();

    }

    private String normalize(String value, String fallback) {

        String normalize = value == null ? "" : value.trim();
        return normalize.isBlank() ? fallback : normalize;

    }

    private String blankToNull(String value) {

        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;

    }

}
