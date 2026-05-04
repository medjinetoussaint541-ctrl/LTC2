package com.team.apk.Service;

import com.team.apk.Dto.PartenaireSearchResultView;
import com.team.apk.Model.Crush;
import com.team.apk.Model.Demande;
import com.team.apk.Model.Persons;
import com.team.apk.Model.StatutLine;
import com.team.apk.Model.Users;
import com.team.apk.Model.Visibilite;
import com.team.apk.Repository.CrushRepository;
import com.team.apk.Repository.DemandeRepository;
import com.team.apk.Repository.RelationsRepository;
import com.team.apk.Repository.UsersRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;


//Service métier dédié à la recherche de partenaires potentiels et à la création d'interactions initiales.
//Il gère la recherche, l'envoi de demandes et la gestion des crushs en appliquant
//les garde-fous nécessaires pour préserver la cohérence métier
@Service
public class PartenaireService {

    private static final String CODE_SELF_REQUEST = "SELF_REQUEST_NOT_ALLOWED";
    private static final String CODE_USER_NOT_FOUND = "USER_NOT_FOUND";
    private static final String CODE_PENDING_REQUEST = "PENDING_REQUEST_ALREADY_EXISTS";
    private static final String CODE_ALREADY_IN_RELATION = "ALREADY_IN_RELATION_WITH_YOU";
    private static final String CODE_RECEIVER_IN_ACTIVE_RELATION = "RECEIVER_ALREADY_IN_ACTIVE_RELATION";
    private static final String CODE_CRUSH_ALREADY_ADDED = "CRUSH_ALREADY_ADDED";

    private final UsersRepository usersRepository;
    private final DemandeRepository demandeRepository;
    private final RelationsRepository relationsRepository;
    private final CrushRepository crushRepository;

    public PartenaireService(UsersRepository usersRepository,
                             DemandeRepository demandeRepository,
                             RelationsRepository relationsRepository,
                             CrushRepository crushRepository) {
        this.usersRepository = usersRepository;
        this.demandeRepository = demandeRepository;
        this.relationsRepository = relationsRepository;
        this.crushRepository = crushRepository;
    }
    
//    Recherche des profils à partir d'un terme libre et projette le résultat 
//    dans une vue adaptée au front
    @Transactional
    public List<PartenaireSearchResultView> search(Users currentUser, String term) {
        if (term == null || term.isBlank() || term.trim().length() < 2) {
            return List.of();
        }

        return usersRepository
                .searchByTermExcluding(term.trim(), currentUser.getUserId())
                .stream()
                .map(candidate -> toSearchResultView(currentUser, candidate))
                .toList();
    }

    @Transactional
    public List<PartenaireSearchResultView> listOnlineUsers(Users currentUser) {
        return usersRepository
                .findByStatutLineExcluding(StatutLine.ONLINE, Visibilite.ON, currentUser.getUserId())
                .stream()
                .map(candidate -> toSearchResultView(currentUser, candidate))
                .toList();
    }
    
//    Tente d'envoyer une demande de relation en vérifiant les conflits métier.
//    Certaines situations renvoient une demande de confirmation explicite plutôt qu'un refus direct.
    @Transactional
    public Map<String, Object> envoyerDemande(Users demandeur,
                                              Long receveurId,
                                              boolean confirmerMalgreRelationActive) {
        // Règle 1 : un utilisateur ne peut pas interagir avec lui-même
        if (demandeur.getUserId().equals(receveurId)) {
            return error(CODE_SELF_REQUEST,
                    "Vous ne pouvez pas vous envoyer une demande à vous-même.");
        }
        // Règle 2 : la cible doit exister
        Users receveur = usersRepository.findById(receveurId).orElse(null);
        if (receveur == null) {
            return error(CODE_USER_NOT_FOUND, "Utilisateur introuvable.");
        }
        // Règle 3 : éviter les doublons de demandes en attente
        if (demandeRepository.existsPendingBetween(demandeur.getUserId(), receveurId)) {
            return error(CODE_PENDING_REQUEST,
                    "Une demande est déjà en attente entre vous deux.");
        }

        if (relationsRepository.findActiveRelationBetweenUsers(
                demandeur.getUserId(), receveurId).isPresent()) {
            return error(CODE_ALREADY_IN_RELATION,
                    "Vous êtes déjà en relation avec cet utilisateur.");
        }

        boolean receveurDejaEnRelation = relationsRepository.existsActiveRelationForUser(receveurId);
        if (receveurDejaEnRelation && !confirmerMalgreRelationActive) {
            return Map.of(
                    "success", false,
                    "requiresConfirmation", true,
                    "canAddCrush", true,
                    "code", CODE_RECEIVER_IN_ACTIVE_RELATION,
                    "message", "Cet utilisateur est déjà dans une relation active. Vous pouvez envoyer la demande quand même, ou l'ajouter directement comme crush."
            );
        }
        // Creation effective de la demande une fois toutes les validations passées.
        Demande demande = new Demande();
        demande.setDemandeur(demandeur);
        demande.setReceveur(receveur);
        demande.setStatut("EN ATTENTE");
        demandeRepository.save(demande);

        String prenom = receveur.getPerson() != null
                ? normalize(receveur.getPerson().getPrenom(), receveur.getEmail())
                : receveur.getEmail();

        return Map.of(
                "success", true,
                "message", "Demande envoyée à " + prenom + " avec succès."
        );
    }
    
    //Ajoute un utilisateur dans la liste des crushs du propriétaire courant.
    @Transactional
    public Map<String, Object> ajouterCrush(Users owner, Long targetId) {
        // interdit l'auto-ajout pour conserver une logique fonctionnelle cohérente.
        if (owner.getUserId().equals(targetId)) {
            return error(CODE_SELF_REQUEST,
                    "Vous ne pouvez pas vous ajouter vous-même comme crush.");
        }

        Users target = usersRepository.findById(targetId).orElse(null);
        if (target == null) {
            return error(CODE_USER_NOT_FOUND, "Utilisateur introuvable.");
        }

        if (relationsRepository.findActiveRelationBetweenUsers(owner.getUserId(), targetId).isPresent()) {
            return error(CODE_ALREADY_IN_RELATION,
                    "Vous êtes déjà en relation avec cet utilisateur.");
        }

        if (crushRepository.existsByOwnerAndTarget(owner.getUserId(), targetId)) {
            return error(CODE_CRUSH_ALREADY_ADDED,
                    "Cet utilisateur est déjà dans votre liste de crushs.");
        }
        
        // Nouveau crush si aucun conflit n'a été détecté.
        Crush crush = new Crush();
        crush.setOwner(owner);
        crush.setTarget(target);
        crush.setStatut("CRUSH");
        crushRepository.save(crush);

        String prenom = target.getPerson() != null
                ? normalize(target.getPerson().getPrenom(), target.getEmail())
                : target.getEmail();

        return Map.of(
                "success", true,
                "message", prenom + " a été ajouté à vos crushs."
        );
    }
    
    //Convertit un utilisateur trouvé en objet de vue enrichi avec des indicateurs métier.
    private PartenaireSearchResultView toSearchResultView(Users currentUser, Users candidate) {
        Persons p = candidate.getPerson();
        String prenom = normalize(p != null ? p.getPrenom() : null, "Utilisateur");
        String nom = normalize(p != null ? p.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = p != null ? blankToNull(p.getPhotoUrl()) : null;

        boolean dejaEnDemande = demandeRepository.existsPendingBetween(
                currentUser.getUserId(), candidate.getUserId());
        boolean dejaEnRelation = relationsRepository.findActiveRelationBetweenUsers(
                currentUser.getUserId(), candidate.getUserId()).isPresent();
        boolean aUneRelationActive = relationsRepository.existsActiveRelationForUser(candidate.getUserId())
                && !dejaEnRelation;
        boolean enLigne = StatutLine.ONLINE.equals(candidate.getStatutLine());
        Crush crush = crushRepository.findByOwnerAndTarget(
                currentUser.getUserId(), candidate.getUserId()).orElse(null);
        boolean crushAjoute = crush != null;
        String crushStatusLabel = crush != null ? humanizeCrushStatus(crush.getStatut()) : null;

        return new PartenaireSearchResultView(
                candidate.getUserId(),
                fullName.isBlank() ? candidate.getEmail() : fullName,
                prenom,
                candidate.getEmail(),
                photoUrl,
                computeInitials(prenom, nom),
                dejaEnDemande,
                dejaEnRelation,
                aUneRelationActive,
                enLigne,
                crushAjoute,
                crushStatusLabel
        );
    }

    private String humanizeCrushStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Crush ajouté";
        }
        return switch (status.trim().toUpperCase()) {
            case "EX_CRUSH" -> "Ex-crush";
            case "CRUSH" -> "Crush ajouté";
            default -> "Crush ajouté";
        };
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of(
                "success", false,
                "code", code,
                "message", message
        );
    }

    private String computeInitials(String firstName, String lastName) {
        String first = blankToNull(firstName);
        String last = blankToNull(lastName);
        StringBuilder sb = new StringBuilder();
        if (first != null) sb.append(Character.toUpperCase(first.charAt(0)));
        if (last != null) sb.append(Character.toUpperCase(last.charAt(0)));
        return sb.isEmpty() ? "U" : sb.toString();
    }

    private String normalize(String value, String fallback) {
        String v = value == null ? "" : value.trim();
        return v.isBlank() ? fallback : v;
    }

    private String blankToNull(String value) {
        String v = value == null ? null : value.trim();
        return (v == null || v.isBlank()) ? null : v;
    }
}
