package com.team.apk.Dto;

public class CrushItemView {

    private final Long id;
    private final String fullName;
    private final String firstName;
    private final String email;
    private final String photoUrl;
    private final String initials;
    private final String relativeTime;
    private final String statusLabel;
    private final boolean canMarkAsExCrush;

    public CrushItemView(Long id,
                         String fullName,
                         String firstName,
                         String email,
                         String photoUrl,
                         String initials,
                         String relativeTime,
                         String statusLabel,
                         boolean canMarkAsExCrush) {
        this.id = id;
        this.fullName = fullName;
        this.firstName = firstName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.initials = initials;
        this.relativeTime = relativeTime;
        this.statusLabel = statusLabel;
        this.canMarkAsExCrush = canMarkAsExCrush;
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

    public String getRelativeTime() {
        return relativeTime;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public boolean isCanMarkAsExCrush() {
        return canMarkAsExCrush;
    }
}
