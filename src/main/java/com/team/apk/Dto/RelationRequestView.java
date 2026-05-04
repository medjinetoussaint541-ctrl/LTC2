package com.team.apk.Dto;

public class RelationRequestView {

    private final Long id;
    private final String fullName;
    private final String firstName;
    private final String email;
    private final String photoUrl;
    private final String initials;
    private final String statusLabel;
    private final String relativeTime;
    private final boolean canAccept;
    private final boolean canDecline;
    private final boolean canCancel;

    public RelationRequestView(Long id,
                               String fullName,
                               String firstName,
                               String email,
                               String photoUrl,
                               String initials,
                               String statusLabel,
                               String relativeTime,
                               boolean canAccept,
                               boolean canDecline,
                               boolean canCancel) {
        this.id = id;
        this.fullName = fullName;
        this.firstName = firstName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.initials = initials;
        this.statusLabel = statusLabel;
        this.relativeTime = relativeTime;
        this.canAccept = canAccept;
        this.canDecline = canDecline;
        this.canCancel = canCancel;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getInitials() {
        return initials;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getRelativeTime() {
        return relativeTime;
    }

    public boolean isCanAccept() {
        return canAccept;
    }

    public boolean isCanDecline() {
        return canDecline;
    }

    public boolean isCanCancel() {
        return canCancel;
    }
}

