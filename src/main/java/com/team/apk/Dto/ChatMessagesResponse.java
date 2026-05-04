package com.team.apk.Dto;

import java.util.List;

public class ChatMessagesResponse {

    private final List<ChatMessageView> messages;
    private final PartnerPresenceView partnerPresence;

    public ChatMessagesResponse(List<ChatMessageView> messages,
                                PartnerPresenceView partnerPresence) {
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.partnerPresence = partnerPresence;
    }

    public List<ChatMessageView> getMessages() {
        return messages;
    }

    public PartnerPresenceView getPartnerPresence() {
        return partnerPresence;
    }
}
