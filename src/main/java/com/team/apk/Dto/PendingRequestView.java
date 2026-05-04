package com.team.apk.Dto;

public class PendingRequestView {

    private final Long id;
    private final String fullName;
    private final String firstName;
    private final String photoUrl;
    private final String initials;
    private final String relativeTime;

    public PendingRequestView(Long id, String fullName, String firstName, String photoUrl, String initials, String relativeTime) {
        this.id = id;
        this.fullName = fullName;
        this.firstName = firstName;
        this.photoUrl = photoUrl;
        this.initials = initials;
        this.relativeTime = relativeTime;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getInitials() {
        return initials;
    }

    public String getRelativeTime() {
        return relativeTime;
    }
}
