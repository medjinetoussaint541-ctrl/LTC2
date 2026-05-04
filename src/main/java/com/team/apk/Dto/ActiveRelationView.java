package com.team.apk.Dto;

public class ActiveRelationView {

    private final Long id;
    private final String partnerName;
    private final String partnerFirstName;
    private final String partnerEmail;
    private final String partnerPhotoUrl;
    private final String partnerInitials;
    private final String statusLabel;
    private final String sinceLabel;
    private final String startDateLabel;
    private final String endDateLabel;
    private final boolean canBreak;

    public ActiveRelationView(Long id,
                              String partnerName,
                              String partnerFirstName,
                              String partnerPhotoUrl,
                              String partnerInitials,
                              String statusLabel,
                              String sinceLabel,
                              String startDateLabel) {
        this(id, partnerName, partnerFirstName, null, partnerPhotoUrl, partnerInitials, statusLabel, sinceLabel, startDateLabel, null, false);
    }

    public ActiveRelationView(Long id,
                              String partnerName,
                              String partnerFirstName,
                              String partnerEmail,
                              String partnerPhotoUrl,
                              String partnerInitials,
                              String statusLabel,
                              String sinceLabel,
                              String startDateLabel,
                              String endDateLabel,
                              boolean canBreak) {
        this.id = id;
        this.partnerName = partnerName;
        this.partnerFirstName = partnerFirstName;
        this.partnerEmail = partnerEmail;
        this.partnerPhotoUrl = partnerPhotoUrl;
        this.partnerInitials = partnerInitials;
        this.statusLabel = statusLabel;
        this.sinceLabel = sinceLabel;
        this.startDateLabel = startDateLabel;
        this.endDateLabel = endDateLabel;
        this.canBreak = canBreak;
    }

    public Long getId() {
        return id;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public String getPartnerFirstName() {
        return partnerFirstName;
    }

    public String getPartnerEmail() {
        return partnerEmail;
    }

    public String getPartnerPhotoUrl() {
        return partnerPhotoUrl;
    }

    public String getPartnerInitials() {
        return partnerInitials;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getSinceLabel() {
        return sinceLabel;
    }

    public String getStartDateLabel() {
        return startDateLabel;
    }

    public String getEndDateLabel() {
        return endDateLabel;
    }

    public boolean isCanBreak() {
        return canBreak;
    }
}
