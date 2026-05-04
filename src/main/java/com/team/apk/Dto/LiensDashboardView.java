package com.team.apk.Dto;

import java.util.List;

public class LiensDashboardView {

    private final CurrentUserView currentUser;
    private final ActiveRelationView activeRelation;
    private final List<ActiveRelationView> exRelations;
    private final List<RelationRequestView> demandesEnvoyees;
    private final List<RelationRequestView> demandesRecues;
    private final List<CrushItemView> crushesAjoutes;

    public LiensDashboardView(CurrentUserView currentUser,
                              ActiveRelationView activeRelation,
                              List<ActiveRelationView> exRelations,
                              List<RelationRequestView> demandesEnvoyees,
                              List<RelationRequestView> demandesRecues,
                              List<CrushItemView> crushesAjoutes) {
        this.currentUser = currentUser;
        this.activeRelation = activeRelation;
        this.exRelations = exRelations;
        this.demandesEnvoyees = demandesEnvoyees;
        this.demandesRecues = demandesRecues;
        this.crushesAjoutes = crushesAjoutes;
    }

    public CurrentUserView getCurrentUser() {
        return currentUser;
    }

    public ActiveRelationView getActiveRelation() {
        return activeRelation;
    }

    public List<ActiveRelationView> getExRelations() {
        return exRelations;
    }

    public List<RelationRequestView> getDemandesEnvoyees() {
        return demandesEnvoyees;
    }

    public List<RelationRequestView> getDemandesRecues() {
        return demandesRecues;
    }

    public List<CrushItemView> getCrushesAjoutes() {
        return crushesAjoutes;
    }
}
