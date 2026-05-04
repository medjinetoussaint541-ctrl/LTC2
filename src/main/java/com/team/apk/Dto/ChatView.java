package com.team.apk.Dto;

import java.util.List;

public class ChatView {

    private final CurrentUserView currentUser;
    private final ActiveRelationView activeRelation;
    private final PartnerPresenceView partnerPresence;
    private final List<ChatMessageView> messages;

    public ChatView(CurrentUserView currentUser,
                    ActiveRelationView activeRelation,
                    PartnerPresenceView partnerPresence,
                    List<ChatMessageView> messages) {
        this.currentUser = currentUser;
        this.activeRelation = activeRelation;
        this.partnerPresence = partnerPresence;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public CurrentUserView getCurrentUser() {
        return currentUser;
    }

    public ActiveRelationView getActiveRelation() {
        return activeRelation;
    }

    public PartnerPresenceView getPartnerPresence() {
        return partnerPresence;
    }

    public List<ChatMessageView> getMessages() {
        return messages;
    }

    public boolean isCanChat() {
        return activeRelation != null;
    }
}
