package com.team.apk.Dto;

public class ProfilView {

    private final CurrentUserView currentUser;
    private final ActiveRelationView relationUserActive;
    private String sexe;

    public ProfilView(CurrentUserView currentUser, ActiveRelationView relationUserActive, String sexe) {

        this.currentUser = currentUser;
        this.relationUserActive = relationUserActive;
        this.sexe = sexe;

    }

    public CurrentUserView getCurrentUserView() {

        return currentUser;

    }

    public ActiveRelationView getRelationUser() {

        return relationUserActive;

    }

    public String getSexe() {
        return sexe;
    }

}
