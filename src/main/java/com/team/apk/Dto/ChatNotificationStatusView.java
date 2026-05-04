package com.team.apk.Dto;

public class ChatNotificationStatusView {

    private final long unreadMessages;

    public ChatNotificationStatusView(long unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public long getUnreadMessages() {
        return unreadMessages;
    }

    public boolean isHasUnreadMessages() {
        return unreadMessages > 0;
    }
}
