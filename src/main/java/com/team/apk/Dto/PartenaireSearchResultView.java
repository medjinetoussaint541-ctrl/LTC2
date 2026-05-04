package com.team.apk.Dto;

public class PartenaireSearchResultView {

    private final Long userId;
    private final String fullName;
    private final String firstName;
    private final String email;
    private final String photoUrl;
    private final String initials;
    private final boolean dejaEnDemande;
    private final boolean dejaEnRelation;
    private final boolean aUneRelationActive;
    private final boolean enLigne;
    private final boolean crushAjoute;
    private final String crushStatusLabel;

    public PartenaireSearchResultView(Long userId,
                                      String fullName,
                                      String firstName,
                                      String email,
                                      String photoUrl,
                                      String initials,
                                      boolean dejaEnDemande,
                                      boolean dejaEnRelation,
                                      boolean aUneRelationActive,
                                      boolean enLigne,
                                      boolean crushAjoute,
                                      String crushStatusLabel) {
        this.userId = userId;
        this.fullName = fullName;
        this.firstName = firstName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.initials = initials;
        this.dejaEnDemande = dejaEnDemande;
        this.dejaEnRelation = dejaEnRelation;
        this.aUneRelationActive = aUneRelationActive;
        this.enLigne = enLigne;
        this.crushAjoute = crushAjoute;
        this.crushStatusLabel = crushStatusLabel;
    }

    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getFirstName() { return firstName; }
    public String getEmail() { return email; }
    public String getPhotoUrl() { return photoUrl; }
    public String getInitials() { return initials; }
    public boolean isDejaEnDemande() { return dejaEnDemande; }
    public boolean isDejaEnRelation() { return dejaEnRelation; }
    public boolean isAUneRelationActive() { return aUneRelationActive; }
    public boolean isEnLigne() { return enLigne; }
    public boolean isCrushAjoute() { return crushAjoute; }
    public String getCrushStatusLabel() { return crushStatusLabel; }
}
