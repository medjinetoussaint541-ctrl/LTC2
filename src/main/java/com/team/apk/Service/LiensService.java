package com.team.apk.Service;

import com.team.apk.Dto.ActiveRelationView;
import com.team.apk.Dto.CrushItemView;
import com.team.apk.Dto.CurrentUserView;
import com.team.apk.Dto.LiensDashboardView;
import com.team.apk.Dto.RelationRequestView;
import com.team.apk.Model.Crush;
import com.team.apk.Model.Demande;
import com.team.apk.Model.Persons;
import com.team.apk.Model.Relations;
import com.team.apk.Model.Users;
import com.team.apk.Repository.CrushRepository;
import com.team.apk.Repository.DemandeRepository;
import com.team.apk.Repository.RelationsRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

//Service métier de la page 'Mes liens'.
//Il assemble les données liées aux relations, aux demandes et aux crushs,
//puis expose les opérations de mise à jour associées.
@Service
public class LiensService {

    private static final Locale LOCALE_FR = Locale.forLanguageTag("fr-FR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE_FR);

    private final DemandeRepository demandeRepository;
    private final CrushRepository crushRepository;
    private final RelationsRepository relationsRepository;

    public LiensService(DemandeRepository demandeRepository,
            CrushRepository crushRepository,
            RelationsRepository relationsRepository) {
        this.demandeRepository = demandeRepository;
        this.crushRepository = crushRepository;
        this.relationsRepository = relationsRepository;
    }

    // Construit la vue complète de la page 'Mes liens'.
    @Transactional
    public LiensDashboardView buildDashboard(Users user) {
        ActiveRelationView activeRelation = relationsRepository.findActiveRelationsForUser(user.getUserId())
                .stream()
                .findFirst()
                .map(relation -> toRelationView(user, relation, true))
                .orElse(null);

        List<ActiveRelationView> exRelations = relationsRepository.findExRelationsForUser(user.getUserId())
                .stream()
                .map(relation -> toRelationView(user, relation, false))
                .toList();

        List<RelationRequestView> demandesEnvoyees = demandeRepository.findSentByUserId(user.getUserId())
                .stream()
                .map(this::toSentRequestView)
                .toList();

        List<RelationRequestView> demandesRecues = demandeRepository.findReceivedByUserId(user.getUserId())
                .stream()
                .map(this::toReceivedRequestView)
                .toList();

        List<CrushItemView> crushesAjoutes = crushRepository.findAddedByOwnerId(user.getUserId())
                .stream()
                .map(this::toCrushItemView)
                .toList();

        return new LiensDashboardView(
                toCurrentUserView(user),
                activeRelation,
                exRelations,
                demandesEnvoyees,
                demandesRecues,
                crushesAjoutes);
    }

    private RelationRequestView toSentRequestView(Demande demande) {
        Users target = demande.getReceveur();
        Persons person = target.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;

        return new RelationRequestView(
                demande.getDemandeId(),
                fullName.isBlank() ? target.getEmail() : fullName,
                prenom,
                target.getEmail(),
                photoUrl,
                computeInitials(prenom, nom),
                humanizeRequestStatus(demande.getStatut()),
                formatRelativeTime(demande.getCreationDate()),
                false,
                false,
                "EN ATTENTE".equalsIgnoreCase(demande.getStatut()));
    }

    private RelationRequestView toReceivedRequestView(Demande demande) {
        Users sender = demande.getDemandeur();
        Persons person = sender.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        boolean pending = "EN ATTENTE".equalsIgnoreCase(demande.getStatut());

        return new RelationRequestView(
                demande.getDemandeId(),
                fullName.isBlank() ? sender.getEmail() : fullName,
                prenom,
                sender.getEmail(),
                photoUrl,
                computeInitials(prenom, nom),
                humanizeRequestStatus(demande.getStatut()),
                formatRelativeTime(demande.getCreationDate()),
                pending,
                pending,
                false);
    }

    private CrushItemView toCrushItemView(Crush crush) {
        Users target = crush.getTarget();
        Persons person = target.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;

        String statusLabel = humanizeCrushStatus(crush.getStatut());
        String relativeTime = "EX_CRUSH".equalsIgnoreCase(crush.getStatut())
                ? (crush.getEndDate() != null
                        ? "Passé en ex-crush " + formatRelativeTime(crush.getEndDate()).toLowerCase()
                        : "Ex-crush")
                : formatRelativeTime(crush.getCreationDate());

        return new CrushItemView(
                crush.getCrushId(),
                fullName.isBlank() ? target.getEmail() : fullName,
                prenom,
                target.getEmail(),
                photoUrl,
                computeInitials(prenom, nom),
                relativeTime,
                statusLabel,
                "CRUSH".equalsIgnoreCase(crush.getStatut()));
    }

    private ActiveRelationView toRelationView(Users currentUser, Relations relation, boolean active) {
        Users partner = Objects.equals(relation.getUser1().getUserId(), currentUser.getUserId())
                ? relation.getUser2()
                : relation.getUser1();

        Persons person = partner.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Partenaire");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        String startDateLabel = relation.getCreationDate() != null ? relation.getCreationDate().format(DATE_FORMATTER)
                : null;
        String endDateLabel = relation.getEndDate() != null ? relation.getEndDate().format(DATE_FORMATTER) : null;
        String sinceLabel = active
                ? (relation.getCreationDate() != null ? "Depuis le " + relation.getCreationDate().format(DATE_FORMATTER)
                        : "Relation active")
                : (relation.getEndDate() != null ? "Rompue " + formatRelativeTime(relation.getEndDate()).toLowerCase()
                        : "Relation terminée");

        return new ActiveRelationView(
                relation.getRelationId(),
                fullName.isBlank() ? partner.getEmail() : fullName,
                prenom,
                partner.getEmail(),
                photoUrl,
                computeInitials(prenom, nom),
                humanizeRelationStatus(relation.getStatut()),
                sinceLabel,
                startDateLabel,
                endDateLabel,
                active && "EN COUPLE".equalsIgnoreCase(relation.getStatut()));
    }

    private CurrentUserView toCurrentUserView(Users user) {
        Persons person = user.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        boolean verified = user.getUserverified();
        String sexe = person != null ? person.getSexe() : null;
        boolean profileVisible = user.getVisibilite() != null && user.getVisibilite() == com.team.apk.Model.Visibilite.ON;  
        return new CurrentUserView(
                user.getUserId(),
                prenom,
                nom,
                fullName.isBlank() ? user.getEmail() : fullName,
                user.getEmail(),
                photoUrl,
                computeInitials(prenom, nom),
                sexe,
                verified,
                profileVisible
        );
    }

    private String humanizeRelationStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Relation";
        }
        return switch (status.trim().toUpperCase()) {
            case "EN COUPLE" -> "En couple";
            case "EX" -> "Ex";
            default -> status;
        };
    }

    private String humanizeRequestStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Inconnue";
        }
        return switch (status.trim().toUpperCase()) {
            case "EN ATTENTE" -> "En attente";
            case "ACCEPTEE" -> "Acceptée";
            case "REFUSEE" -> "Refusée";
            case "ANNULEE" -> "Annulée";
            default -> status;
        };
    }

    private String humanizeCrushStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Crush";
        }
        return switch (status.trim().toUpperCase()) {
            case "EX_CRUSH" -> "Ex-crush";
            case "CRUSH" -> "Crush";
            default -> status;
        };
    }

    // Annule une demande envoyée tant qu'elle est encore en attente
    @Transactional
    public java.util.Map<String, Object> cancelSentPendingRequest(Long currentUserId, Long demandeId) {
        Demande demande = demandeRepository.findByDemandeIdAndDemandeurUserId(demandeId, currentUserId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Demande introuvable."));

        if (!"EN ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Seules les demandes en attente peuvent être annulées.");
        }

        demande.setStatut("ANNULEE");
        demandeRepository.save(demande);

        return java.util.Map.of(
                "success", true,
                "message", "Demande annulée avec succès.",
                "statusLabel", humanizeRequestStatus(demande.getStatut()));
    }

    // Bascule un crush existant vers le statut d'ex-crush
    @Transactional
    public Map<String, Object> markCrushAsExCrush(Long currentUserId, Long crushId) {
        Crush crush = crushRepository.findByCrushIdAndOwnerUserId(crushId, currentUserId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Crush introuvable."));

        if (!"CRUSH".equalsIgnoreCase(crush.getStatut())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Seuls les crushs actifs peuvent être passés en ex-crush.");
        }

        Users target = crush.getTarget();
        Persons person = target.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String targetName = (prenom + " " + nom).trim();
        if (targetName.isBlank()) {
            targetName = target.getEmail();
        }

        crush.setStatut("EX_CRUSH");
        crush.setEndDate(LocalDateTime.now());
        crushRepository.save(crush);

        return Map.of(
                "success", true,
                "message", targetName + " a été passé en ex-crush.",
                "statusLabel", humanizeCrushStatus(crush.getStatut()));
    }

    // Met fin à une relation active appartenant à l'utilisateur courant
    @Transactional
    public Map<String, Object> breakActiveRelation(Long currentUserId, Long relationId) {
        Relations relation = relationsRepository.findOwnedByRelationIdAndUserId(relationId, currentUserId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Relation introuvable."));

        if (!"EN COUPLE".equalsIgnoreCase(relation.getStatut())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Seule une relation active peut être rompue.");
        }

        Users partner = Objects.equals(relation.getUser1().getUserId(), currentUserId)
                ? relation.getUser2()
                : relation.getUser1();

        Persons person = partner.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Partenaire");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String partnerName = (prenom + " " + nom).trim();
        if (partnerName.isBlank()) {
            partnerName = partner.getEmail();
        }

        relation.setStatut("EX");
        relation.setEndDate(LocalDateTime.now());
        relationsRepository.save(relation);

        return Map.of(
                "success", true,
                "message", "La relation avec " + partnerName + " a été rompue.",
                "statusLabel", humanizeRelationStatus(relation.getStatut()));
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "À l'instant";
        }

        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long minutes = Math.max(0, duration.toMinutes());
        long hours = Math.max(0, duration.toHours());
        long days = Math.max(0, duration.toDays());

        if (minutes < 1) {
            return "À l'instant";
        }
        if (minutes < 60) {
            return "Il y a " + minutes + " min";
        }
        if (hours < 24) {
            return "Il y a " + hours + " h";
        }
        if (days < 30) {
            return "Il y a " + days + " j";
        }
        long months = Math.max(1, days / 30);
        if (months < 12) {
            return "Il y a " + months + " mois";
        }
        long years = Math.max(1, days / 365);
        return "Il y a " + years + " an" + (years > 1 ? "s" : "");
    }

    private String computeInitials(String firstName, String lastName) {
        String first = blankToNull(firstName);
        String last = blankToNull(lastName);
        StringBuilder sb = new StringBuilder();
        if (first != null)
            sb.append(Character.toUpperCase(first.charAt(0)));
        if (last != null)
            sb.append(Character.toUpperCase(last.charAt(0)));
        return sb.isEmpty() ? "U" : sb.toString();
    }

    private String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String blankToNull(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
