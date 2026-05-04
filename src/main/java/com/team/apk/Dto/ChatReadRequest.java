package com.team.apk.Dto;

public class ChatReadRequest {

    private Long lastVisibleMessageId;

    public Long getLastVisibleMessageId() {
        return lastVisibleMessageId;
    }

    public void setLastVisibleMessageId(Long lastVisibleMessageId) {
        this.lastVisibleMessageId = lastVisibleMessageId;
    }
}
