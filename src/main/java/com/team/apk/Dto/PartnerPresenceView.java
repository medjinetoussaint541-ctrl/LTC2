package com.team.apk.Dto;

public class PartnerPresenceView {

    private final boolean online;
    private final String statusLabel;
    private final String detailLabel;
    private final String lastSeenDateTimeLabel;

    public PartnerPresenceView(boolean online,
                               String statusLabel,
                               String detailLabel,
                               String lastSeenDateTimeLabel) {
        this.online = online;
        this.statusLabel = statusLabel;
        this.detailLabel = detailLabel;
        this.lastSeenDateTimeLabel = lastSeenDateTimeLabel;
    }

    public boolean isOnline() {
        return online;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getDetailLabel() {
        return detailLabel;
    }

    public String getLastSeenDateTimeLabel() {
        return lastSeenDateTimeLabel;
    }
}
