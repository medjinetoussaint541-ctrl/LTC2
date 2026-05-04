package com.team.apk.Service;

import com.team.apk.Dto.AccueilDashboardView;
import com.team.apk.Dto.ActiveRelationView;
import com.team.apk.Dto.ActivityItemView;
import com.team.apk.Dto.CurrentUserView;
import com.team.apk.Dto.DashboardStatsView;
import com.team.apk.Dto.PendingRequestView;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccueilService {

    private static final Locale LOCALE_FR = Locale.forLanguageTag("fr-FR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE_FR);

    private final DemandeRepository demandeRepository;
    private final RelationsRepository relationsRepository;
    private final CrushRepository crushRepository;
    private final ChatService chatService;

    public AccueilService(DemandeRepository demandeRepository,
            RelationsRepository relationsRepository,
            CrushRepository crushRepository,
            ChatService chatService) {
        this.demandeRepository = demandeRepository;
        this.relationsRepository = relationsRepository;
        this.crushRepository = crushRepository;
        this.chatService = chatService;
    }

    @Transactional
    public AccueilDashboardView buildDashboard(Users user) {
        CurrentUserView currentUserView = toCurrentUserView(user);

        List<Demande> receivedDemandes = demandeRepository.findReceivedByUserId(user.getUserId());
        List<Demande> pendingDemandes = receivedDemandes.stream()
                .filter(demande -> "EN ATTENTE".equalsIgnoreCase(demande.getStatut()))
                .toList();

        List<PendingRequestView> pendingRequestViews = pendingDemandes.stream()
                .limit(5)
                .map(this::toPendingRequestView)
                .toList();

        List<Demande> sentDemandes = demandeRepository.findSentByUserId(user.getUserId());
        List<Crush> receivedCrushes = crushRepository.findActiveReceivedByTargetId(user.getUserId());
        List<Relations> endedRelations = relationsRepository.findExRelationsForUser(user.getUserId());

        ActiveRelationView activeRelationView = relationsRepository.findActiveRelationsForUser(user.getUserId())
                .stream()
                .findFirst()
                .map(relation -> toActiveRelationView(user, relation))
                .orElse(null);

        // Construction du flux d'activité avec une limite de 4 pour l'accueil
        List<ActivityItemView> activityItems = buildActivityFeed(user, sentDemandes, receivedDemandes, receivedCrushes, endedRelations,
                activeRelationView, 4);

        long relationsCount = relationsRepository.countActiveRelationsForUser(user.getUserId());
        long demandesCount = demandeRepository.countPendingReceivedByUserId(user.getUserId());
        long historiqueCount = Math.max(
                Math.max(activityItems.size(), demandeRepository.countInteractionsForUser(user.getUserId())),
                relationsRepository.countAllRelationsForUser(user.getUserId()));
        long crushsAjoutesCount = crushRepository.countActiveAddedByOwnerId(user.getUserId());
        long crushsRecusCount = crushRepository.countActiveReceivedForTargetId(user.getUserId());
        long unreadMessagesCount = chatService.countUnreadMessagesForUser(user.getUserId());

        DashboardStatsView statsView = new DashboardStatsView(
                relationsCount,
                demandesCount,
                historiqueCount,
                crushsAjoutesCount,
                crushsRecusCount,
                unreadMessagesCount);

        return new AccueilDashboardView(currentUserView, statsView, pendingRequestViews, activityItems,
                activeRelationView);
    }

    // Nouvelle méthode pour l'historique complet (sans limite)
    @Transactional
    public List<ActivityItemView> buildFullActivityHistory(Users user) {
        List<Demande> receivedDemandes = demandeRepository.findReceivedByUserId(user.getUserId());
        List<Demande> sentDemandes = demandeRepository.findSentByUserId(user.getUserId());
        List<Crush> receivedCrushes = crushRepository.findActiveReceivedByTargetId(user.getUserId());
        List<Relations> endedRelations = relationsRepository.findExRelationsForUser(user.getUserId());

        ActiveRelationView activeRelationView = relationsRepository.findActiveRelationsForUser(user.getUserId())
                .stream()
                .findFirst()
                .map(relation -> toActiveRelationView(user, relation))
                .orElse(null);

        return buildActivityFeed(user, sentDemandes, receivedDemandes, receivedCrushes, endedRelations, activeRelationView,
                Integer.MAX_VALUE);
    }

    @Transactional
    public Map<String, Object> acceptPendingRequest(Long currentUserId, Long demandeId) {
        Demande demande = loadOwnedPendingRequest(currentUserId, demandeId);
        Long demandeurId = demande.getDemandeur().getUserId();

        if (relationsRepository.countActiveRelationsForUser(currentUserId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vous avez déjà une relation active. Vous ne pouvez pas accepter une autre demande.");
        }
        if (relationsRepository.countActiveRelationsForUser(demandeurId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette demande ne peut pas être acceptée car cet utilisateur a déjà une relation active.");
        }
        if (relationsRepository.findActiveRelationBetweenUsers(currentUserId, demandeurId).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une relation active existe déjà entre vous.");
        }

        demande.setStatut("ACCEPTEE");
        demandeRepository.save(demande);

        Relations relation = new Relations();
        relation.setUser1(demande.getDemandeur());
        relation.setUser2(demande.getReceveur());
        relation.setStatut("EN COUPLE");
        relationsRepository.save(relation);

        return Map.of("success", true, "message", "Demande acceptée avec succès.");
    }

    @Transactional
    public Map<String, Object> declinePendingRequest(Long currentUserId, Long demandeId) {
        Demande demande = loadOwnedPendingRequest(currentUserId, demandeId);
        demande.setStatut("REFUSEE");
        demandeRepository.save(demande);
        return Map.of("success", true, "message", "Demande refusée avec succès.");
    }

    private Demande loadOwnedPendingRequest(Long currentUserId, Long demandeId) {
        Demande demande = demandeRepository.findByDemandeIdAndReceveurUserId(demandeId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (!"EN ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée.");
        }
        return demande;
    }

    private CurrentUserView toCurrentUserView(Users user) {
        Persons person = user.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        boolean profileVisible = user.getVisibilite() != null && user.getVisibilite() == com.team.apk.Model.Visibilite.ON;
        String sexe = person != null ? person.getSexe() : null;
        boolean verified = user.getUserverified();
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

    private PendingRequestView toPendingRequestView(Demande demande) {
        Persons person = demande.getDemandeur().getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        return new PendingRequestView(
                demande.getDemandeId(),
                fullName,
                prenom,
                photoUrl,
                computeInitials(prenom, nom),
                formatRelativeTime(demande.getCreationDate()));
    }

    private ActiveRelationView toActiveRelationView(Users currentUser, Relations relation) {
        Users partner = Objects.equals(relation.getUser1().getUserId(), currentUser.getUserId())
                ? relation.getUser2()
                : relation.getUser1();
        Persons partnerPerson = partner.getPerson();
        String prenom = normalize(partnerPerson != null ? partnerPerson.getPrenom() : null, "Partenaire");
        String nom = normalize(partnerPerson != null ? partnerPerson.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = partnerPerson != null ? blankToNull(partnerPerson.getPhotoUrl()) : null;
        return new ActiveRelationView(
                relation.getRelationId(),
                fullName,
                prenom,
                photoUrl,
                computeInitials(prenom, nom),
                humanizeRelationStatus(relation.getStatut()),
                "Depuis le " + relation.getCreationDate().format(DATE_FORMATTER),
                relation.getCreationDate().format(DATE_FORMATTER));
    }

    private List<ActivityItemView> buildActivityFeed(Users currentUser,
            List<Demande> sentDemandes,
            List<Demande> receivedDemandes,
            List<Crush> receivedCrushes,
            List<Relations> endedRelations,
            ActiveRelationView activeRelationView,
            int limit) {
        List<ActivityItemView> items = new ArrayList<>();

        if (activeRelationView != null) {
            LocalDateTime now = LocalDateTime.now();
            items.add(new ActivityItemView(
                    "Relation active",
                    "Avec " + activeRelationView.getPartnerName(),
                    activeRelationView.getSinceLabel(),
                    "favorite",
                    "rose",
                    now.minusSeconds(1).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
        }

        sentDemandes.stream()
                .limit(6)
                .map(this::toSentActivityItem)
                .forEach(items::add);

        receivedDemandes.stream()
                .limit(6)
                .map(this::toReceivedActivityItem)
                .forEach(items::add);

        receivedCrushes.stream()
                .limit(6)
                .map(this::toReceivedCrushActivityItem)
                .forEach(items::add);
        
        endedRelations.stream()
            .limit(6)
            .map(relation -> toEndedRelationActivityItem(currentUser, relation))
            .forEach(items::add);

        if (currentUser.getCreationDate() != null) {
            items.add(new ActivityItemView(
                    "Compte créé",
                    "Bienvenue sur LTC App",
                    formatRelativeTime(currentUser.getCreationDate()),
                    "shield_person",
                    "purple",
                    currentUser.getCreationDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
        }

        return items.stream()
                .sorted(Comparator.comparingLong(ActivityItemView::getSortKey).reversed())
                .limit(limit)
                .toList();
    }

    private ActivityItemView toSentActivityItem(Demande demande) {
        String targetName = displayName(demande.getReceveur().getPerson(), demande.getReceveur().getEmail());
        LocalDateTime activityDate = resolveDemandeActivityDate(demande);
        String status = normalizeStatus(demande.getStatut());

        return switch (status) {
            case "ACCEPTEE" -> new ActivityItemView(
                    "Demande acceptée",
                    targetName + " a accepté votre demande",
                    formatRelativeTime(activityDate),
                    "check_circle",
                    "rose",
                    toEpochMilli(activityDate));
            case "REFUSEE" -> new ActivityItemView(
                    "Demande refusée",
                    targetName + " a refusé votre demande",
                    formatRelativeTime(activityDate),
                    "cancel",
                    "gold",
                    toEpochMilli(activityDate));
            case "ANNULEE" -> new ActivityItemView(
                    "Demande annulée",
                    "Vous avez annulé la demande envoyée à " + targetName,
                    formatRelativeTime(activityDate),
                    "close",
                    "purple",
                    toEpochMilli(activityDate));
            default -> new ActivityItemView(
                    "Demande envoyée",
                    "À " + targetName,
                    formatRelativeTime(activityDate),
                    "send",
                    "teal",
                    toEpochMilli(activityDate));
        };
    }

    private ActivityItemView toReceivedActivityItem(Demande demande) {
        String senderName = displayName(demande.getDemandeur().getPerson(), demande.getDemandeur().getEmail());
        LocalDateTime activityDate = resolveDemandeActivityDate(demande);
        String status = normalizeStatus(demande.getStatut());

        return switch (status) {
            case "ACCEPTEE" -> new ActivityItemView(
                    "Demande acceptée",
                    "Vous avez accepté la demande de " + senderName,
                    formatRelativeTime(activityDate),
                    "check_circle",
                    "rose",
                    toEpochMilli(activityDate));
            case "REFUSEE" -> new ActivityItemView(
                    "Demande refusée",
                    "Vous avez refusé la demande de " + senderName,
                    formatRelativeTime(activityDate),
                    "cancel",
                    "gold",
                    toEpochMilli(activityDate));
            case "ANNULEE" -> new ActivityItemView(
                    "Demande annulée",
                    senderName + " a annulé sa demande",
                    formatRelativeTime(activityDate),
                    "close",
                    "purple",
                    toEpochMilli(activityDate));
            default -> new ActivityItemView(
                    "Nouvelle demande reçue",
                    "De " + senderName,
                    formatRelativeTime(activityDate),
                    "mark_email_read",
                    "gold",
                    toEpochMilli(activityDate));
        };
    }

    private ActivityItemView toReceivedCrushActivityItem(Crush crush) {
        LocalDateTime activityDate = crush.getCreationDate();
        return new ActivityItemView(
                "Nouveau crush reçu",
                "Quelqu'un vous a ajouté comme crush.",
                formatRelativeTime(activityDate),
                "favorite",
                "purple",
                toEpochMilli(activityDate));
    }
    
    private ActivityItemView toEndedRelationActivityItem(Users currentUser, Relations relation) {
        Users partner = Objects.equals(relation.getUser1().getUserId(), currentUser.getUserId())
                ? relation.getUser2()
                : relation.getUser1();

        String partnerName = displayName(partner.getPerson(), partner.getEmail());
        LocalDateTime activityDate = relation.getEndDate() != null
                ? relation.getEndDate()
                : relation.getCreationDate();

        return new ActivityItemView(
                "Relation rompue",
                "La relation avec " + partnerName + " a pris fin",
                formatRelativeTime(activityDate),
                "heart_broken",
                "gold",
                toEpochMilli(activityDate));
    }

    private LocalDateTime resolveDemandeActivityDate(Demande demande) {
        return demande.getUpdateDate() != null ? demande.getUpdateDate() : demande.getCreationDate();
    }

    private long toEpochMilli(LocalDateTime value) {
        return value == null ? 0L : value.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private String displayName(Persons person, String fallbackEmail) {
        if (person == null)
            return fallbackEmail;
        String fullName = (normalize(person.getPrenom(), "") + " " + normalize(person.getNom(), "")).trim();
        return fullName.isBlank() ? fallbackEmail : fullName;
    }

    private String formatRelativeTime(LocalDateTime value) {
        if (value == null)
            return "À l'instant";
        Duration duration = Duration.between(value, LocalDateTime.now());
        long minutes = Math.max(duration.toMinutes(), 0);

        if (minutes < 1)
            return "À l'instant";
        if (minutes < 60)
            return "Il y a " + minutes + " min";
        long hours = duration.toHours();
        if (hours < 24)
            return hours == 1 ? "Il y a 1 h" : "Il y a " + hours + " h";
        long days = duration.toDays();
        if (days < 7)
            return days == 1 ? "Hier" : "Il y a " + days + " jours";
        long weeks = days / 7;
        if (weeks < 5)
            return weeks == 1 ? "Il y a 1 semaine" : "Il y a " + weeks + " semaines";
        return value.format(DATE_FORMATTER);
    }

    private String humanizeRelationStatus(String status) {
        if (status == null)
            return "Relation active";
        return switch (status.trim().toUpperCase()) {
            case "EN COUPLE" -> "En couple";
            case "EX" -> "Ex";
            default -> status;
        };
    }

    private String computeInitials(String firstName, String lastName) {
        String first = blankToNull(firstName);
        String last = blankToNull(lastName);
        StringBuilder builder = new StringBuilder();
        if (first != null)
            builder.append(Character.toUpperCase(first.charAt(0)));
        if (last != null)
            builder.append(Character.toUpperCase(last.charAt(0)));
        return builder.isEmpty() ? "U" : builder.toString();
    }

    private String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private String blankToNull(String value) {
        String normalized = value == null ? null : value.trim();
        return (normalized == null || normalized.isBlank()) ? null : normalized;
    }
}
