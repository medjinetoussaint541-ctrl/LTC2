package com.team.apk.Dto;

public class DashboardStatsView {

    private final long relations;
    private final long demandes;
    private final long historique;
    private final long crushsAjoutes;
    private final long crushsRecus;
    private final long unreadMessages;

    public DashboardStatsView(long relations,
                              long demandes,
                              long historique,
                              long crushsAjoutes,
                              long crushsRecus,
                              long unreadMessages) {
        this.relations = relations;
        this.demandes = demandes;
        this.historique = historique;
        this.crushsAjoutes = crushsAjoutes;
        this.crushsRecus = crushsRecus;
        this.unreadMessages = unreadMessages;
    }

    public long getRelations() {
        return relations;
    }

    public long getDemandes() {
        return demandes;
    }

    public long getHistorique() {
        return historique;
    }

    public long getCrushsAjoutes() {
        return crushsAjoutes;
    }

    public long getCrushsRecus() {
        return crushsRecus;
    }

    public long getUnreadMessages() {
        return unreadMessages;
    }

    public long getNotificationCount() {
        return demandes + unreadMessages;
    }
}
