package com.team.apk.Service;

import com.team.apk.Dto.ActiveRelationView;
import com.team.apk.Dto.ChatMessageView;
import com.team.apk.Dto.ChatNotificationStatusView;
import com.team.apk.Dto.ChatView;
import com.team.apk.Dto.CurrentUserView;
import com.team.apk.Dto.PartnerPresenceView;
import com.team.apk.Model.ChatMessage;
import com.team.apk.Model.Persons;
import com.team.apk.Model.Relations;
import com.team.apk.Model.StatutLine;
import com.team.apk.Model.Users;
import com.team.apk.Repository.ChatMessageRepository;
import com.team.apk.Repository.RelationsRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private static final Locale LOCALE_FR = Locale.forLanguageTag("fr-FR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE_FR);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", LOCALE_FR);
    private static final DateTimeFormatter TIME_WITH_DAY_FORMATTER = DateTimeFormatter.ofPattern("d MMM · HH:mm",
            LOCALE_FR);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm",
            LOCALE_FR);
    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final RelationsRepository relationsRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageEncryptionService chatMessageEncryptionService;

    public ChatService(RelationsRepository relationsRepository,
            ChatMessageRepository chatMessageRepository,
            ChatMessageEncryptionService chatMessageEncryptionService) {
        this.relationsRepository = relationsRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageEncryptionService = chatMessageEncryptionService;
    }

    @Transactional
    public ChatView buildChatView(Users user) {
        CurrentUserView currentUserView = toCurrentUserView(user);
        Relations activeRelation = findActiveRelation(user.getUserId()).orElse(null);

        if (activeRelation == null) {
            return new ChatView(currentUserView, null, null, List.of());
        }
        Users partner = resolvePartner(activeRelation, user.getUserId());
        Long lastReadMessageId = getLastReadMessageId(activeRelation, user.getUserId());

        List<ChatMessageView> messages = chatMessageRepository.findConversationMessages(activeRelation.getRelationId())
                .stream()
                .map(message -> toMessageView(user.getUserId(), lastReadMessageId, message))
                .toList();

        return new ChatView(
                currentUserView,
                toActiveRelationView(user, activeRelation),
                toPartnerPresenceView(partner),
                messages);
    }

    @Transactional
    public List<ChatMessageView> loadMessages(Long currentUserId, Long afterMessageId) {
        Relations activeRelation = loadActiveRelationForUser(currentUserId);
        Long lastReadMessageId = getLastReadMessageId(activeRelation, currentUserId);
        List<ChatMessage> messages = afterMessageId == null
                ? chatMessageRepository.findConversationMessages(activeRelation.getRelationId())
                : chatMessageRepository.findConversationMessagesAfter(activeRelation.getRelationId(), afterMessageId);

        return messages.stream()
                .map(message -> toMessageView(currentUserId, lastReadMessageId, message))
                .toList();
    }

    @Transactional
    public PartnerPresenceView loadPartnerPresence(Long currentUserId) {
        Relations activeRelation = loadActiveRelationForUser(currentUserId);
        Users partner = resolvePartner(activeRelation, currentUserId);
        return toPartnerPresenceView(partner);
    }

    @Transactional
    public ChatMessageView sendMessage(Long currentUserId, String rawContent) {
        String content = normalizeMessage(rawContent);
        Relations activeRelation = loadActiveRelationForUser(currentUserId);
        Users sender = resolveCurrentUser(activeRelation, currentUserId);

        ChatMessageEncryptionService.EncryptedPayload encryptedPayload = chatMessageEncryptionService.encrypt(content);

        ChatMessage message = new ChatMessage();
        message.setRelation(activeRelation);
        message.setSender(sender);
        message.setMessageIv(encryptedPayload.ivBase64());
        message.setCipherText(encryptedPayload.cipherTextBase64());

        ChatMessage savedMessage = chatMessageRepository.save(message);
        return toMessageView(currentUserId, getLastReadMessageId(activeRelation, currentUserId), savedMessage);
    }

    @Transactional
    public ChatNotificationStatusView loadNotificationStatus(Long currentUserId) {
        return new ChatNotificationStatusView(countUnreadMessages(currentUserId));
    }

    @Transactional
    public long countUnreadMessagesForUser(Long currentUserId) {
        return countUnreadMessages(currentUserId);
    }

    @Transactional
    public ChatNotificationStatusView markConversationAsReadUpTo(Long currentUserId, Long lastVisibleMessageId) {
        if (lastVisibleMessageId == null) {
            return loadNotificationStatus(currentUserId);
        }

        Relations activeRelation = findActiveRelation(currentUserId).orElse(null);
        if (activeRelation == null) {
            return new ChatNotificationStatusView(0);
        }

        Long messageIdToMark = chatMessageRepository.findLatestPartnerMessageIdUpTo(
                activeRelation.getRelationId(),
                currentUserId,
                lastVisibleMessageId);

        if (messageIdToMark == null) {
            return new ChatNotificationStatusView(countUnreadMessages(currentUserId, activeRelation));
        }

        Long currentLastReadMessageId = getLastReadMessageId(activeRelation, currentUserId);
        if (currentLastReadMessageId == null || messageIdToMark > currentLastReadMessageId) {
            setLastReadMessageId(activeRelation, currentUserId, messageIdToMark);
            relationsRepository.save(activeRelation);
        }

        return new ChatNotificationStatusView(countUnreadMessages(currentUserId, activeRelation));
    }

    private java.util.Optional<Relations> findActiveRelation(Long userId) {
        return relationsRepository.findActiveRelationsForUser(userId)
                .stream()
                .findFirst();
    }

    private Relations loadActiveRelationForUser(Long userId) {
        return findActiveRelation(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Aucune relation active. Le chat est réservé aux utilisateurs actuellement en relation."));
    }

    private ChatMessageView toMessageView(Long currentUserId, Long lastReadMessageId, ChatMessage message) {
        Users sender = message.getSender();
        Persons person = sender.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String senderName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        boolean mine = Objects.equals(sender.getUserId(), currentUserId);
        boolean newForCurrentUser = !mine
                && message.getMessageId() != null
                && (lastReadMessageId == null || message.getMessageId() > lastReadMessageId);
        LocalDateTime creationDate = message.getCreationDate();

        return new ChatMessageView(
                message.getMessageId(),
                chatMessageEncryptionService.decrypt(message.getMessageIv(), message.getCipherText()),
                mine ? "Vous" : (senderName.isBlank() ? sender.getEmail() : senderName),
                photoUrl,
                computeInitials(prenom, nom),
                mine,
                newForCurrentUser,
                formatTimeLabel(creationDate),
                creationDate != null ? creationDate.format(DATE_TIME_FORMATTER) : "");
    }

    private ActiveRelationView toActiveRelationView(Users currentUser, Relations relation) {
        Users partner = resolvePartner(relation, currentUser.getUserId());
        Persons person = partner.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Partenaire");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        String startDateLabel = relation.getCreationDate() != null
                ? relation.getCreationDate().format(DATE_FORMATTER)
                : null;
        String sinceLabel = relation.getCreationDate() != null
                ? "Depuis le " + relation.getCreationDate().format(DATE_FORMATTER)
                : "Relation active";

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
                null,
                false);
    }

    private PartnerPresenceView toPartnerPresenceView(Users partner) {
        boolean online = StatutLine.ONLINE.equals(partner.getStatutLine());
        LocalDateTime lastSeen = partner.getLastSeen();
        String detailLabel = online
                ? "En ligne maintenant"
                : (lastSeen != null
                        ? "Vu le " + lastSeen.format(DATE_TIME_FORMATTER)
                        : "Aucune présence récente enregistrée");

        return new PartnerPresenceView(
                online,
                online ? "En ligne" : "Hors ligne",
                detailLabel,
                lastSeen != null ? lastSeen.format(DATE_TIME_FORMATTER) : null);
    }

    private CurrentUserView toCurrentUserView(Users user) {
        Persons person = user.getPerson();
        String prenom = normalize(person != null ? person.getPrenom() : null, "Utilisateur");
        String nom = normalize(person != null ? person.getNom() : null, "");
        String fullName = (prenom + " " + nom).trim();
        String photoUrl = person != null ? blankToNull(person.getPhotoUrl()) : null;
        String sexe = person != null ? person.getSexe() : null;
        boolean userverified = user.getUserverified();
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
                userverified,
                profileVisible);
    }

    private Users resolveCurrentUser(Relations relation, Long currentUserId) {
        if (Objects.equals(relation.getUser1().getUserId(), currentUserId)) {
            return relation.getUser1();
        }
        if (Objects.equals(relation.getUser2().getUserId(), currentUserId)) {
            return relation.getUser2();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Vous n'êtes pas autorisé à utiliser cette conversation.");
    }

    private Users resolvePartner(Relations relation, Long currentUserId) {
        return Objects.equals(relation.getUser1().getUserId(), currentUserId)
                ? relation.getUser2()
                : relation.getUser1();
    }

    private long countUnreadMessages(Long currentUserId) {
        Relations activeRelation = findActiveRelation(currentUserId).orElse(null);
        return activeRelation == null ? 0 : countUnreadMessages(currentUserId, activeRelation);
    }

    private long countUnreadMessages(Long currentUserId, Relations activeRelation) {
        return chatMessageRepository.countUnreadMessages(
                activeRelation.getRelationId(),
                currentUserId,
                getLastReadMessageId(activeRelation, currentUserId));
    }

    private Long getLastReadMessageId(Relations relation, Long currentUserId) {
        if (Objects.equals(relation.getUser1().getUserId(), currentUserId)) {
            return relation.getUser1LastReadMessageId();
        }
        if (Objects.equals(relation.getUser2().getUserId(), currentUserId)) {
            return relation.getUser2LastReadMessageId();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Vous n'êtes pas autorisé à utiliser cette conversation.");
    }

    private void setLastReadMessageId(Relations relation, Long currentUserId, Long messageId) {
        if (Objects.equals(relation.getUser1().getUserId(), currentUserId)) {
            relation.setUser1LastReadMessageId(messageId);
            return;
        }
        if (Objects.equals(relation.getUser2().getUserId(), currentUserId)) {
            relation.setUser2LastReadMessageId(messageId);
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Vous n'êtes pas autorisé à utiliser cette conversation.");
    }

    private String normalizeMessage(String rawContent) {
        String content = rawContent == null ? "" : rawContent.strip();
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le message ne peut pas être vide.");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un message ne peut pas dépasser 1000 caractères.");
        }
        return content;
    }

    private String formatTimeLabel(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "À l'instant";
        }
        return LocalDate.from(dateTime).equals(LocalDate.now())
                ? dateTime.format(TIME_FORMATTER)
                : dateTime.format(TIME_WITH_DAY_FORMATTER);
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

    private String computeInitials(String firstName, String lastName) {
        String first = blankToNull(firstName);
        String last = blankToNull(lastName);
        StringBuilder sb = new StringBuilder();
        if (first != null) {
            sb.append(Character.toUpperCase(first.charAt(0)));
        }
        if (last != null) {
            sb.append(Character.toUpperCase(last.charAt(0)));
        }
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
