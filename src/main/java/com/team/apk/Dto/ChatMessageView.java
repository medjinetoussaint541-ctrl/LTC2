package com.team.apk.Dto;

public class ChatMessageView {

    private final Long id;
    private final String content;
    private final String senderName;
    private final String senderPhotoUrl;
    private final String senderInitials;
    private final boolean mine;
    private final boolean newForCurrentUser;
    private final String timeLabel;
    private final String dateTimeLabel;

    public ChatMessageView(Long id,
            String content,
            String senderName,
            String senderPhotoUrl,
            String senderInitials,
            boolean mine,
            boolean newForCurrentUser,
            String timeLabel,
            String dateTimeLabel) {
        this.id = id;
        this.content = content;
        this.senderName = senderName;
        this.senderPhotoUrl = senderPhotoUrl;
        this.senderInitials = senderInitials;
        this.mine = mine;
        this.newForCurrentUser = newForCurrentUser;
        this.timeLabel = timeLabel;
        this.dateTimeLabel = dateTimeLabel;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderPhotoUrl() {
        return senderPhotoUrl;
    }

    public String getSenderInitials() {
        return senderInitials;
    }

    public boolean isMine() {
        return mine;
    }

    public boolean isNewForCurrentUser() {
        return newForCurrentUser;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public String getDateTimeLabel() {
        return dateTimeLabel;
    }
}
