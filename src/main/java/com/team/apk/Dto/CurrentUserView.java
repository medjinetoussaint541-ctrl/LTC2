package com.team.apk.Dto;

public class CurrentUserView {

    private final Long id;
    private final String prenom;
    private final String nom;
    private final String fullName;
    private final String email;
    private final String photoUrl;
    private final String initials;
    private String sexe;
    private final boolean userverified;
    private final boolean profileVisible;

    public CurrentUserView(Long id,
            String prenom,
            String nom,
            String fullName,
            String email,
            String photoUrl,
            String initials,
            String sexe,
            boolean userverified,
            boolean profileVisible) 
            {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.fullName = fullName;
        this.email = email;
        this.sexe = sexe;
        this.photoUrl = photoUrl;
        this.initials = initials;
        this.userverified = userverified;
        this.profileVisible = profileVisible;
    }

    public Long getId() {
        return id;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getNom() {
        return nom;
    }

    public String getFullName() {
        return fullName;
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

    public String getSexe() {
        return sexe;
    }

    public boolean getUserverified() {
        return this.userverified;
    }

    public boolean isProfileVisible() {
        return profileVisible;
    }
}
