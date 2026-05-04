package com.team.apk.Dto;

import java.util.List;

public class AccueilDashboardView {

    private final CurrentUserView currentUser;
    private final DashboardStatsView stats;
    private final List<PendingRequestView> pendingRequests;
    private final List<ActivityItemView> activityItems;
    private final ActiveRelationView activeRelation;

    public AccueilDashboardView(CurrentUserView currentUser,
                                DashboardStatsView stats,
                                List<PendingRequestView> pendingRequests,
                                List<ActivityItemView> activityItems,
                                ActiveRelationView activeRelation) {
        this.currentUser = currentUser;
        this.stats = stats;
        this.pendingRequests = pendingRequests;
        this.activityItems = activityItems;
        this.activeRelation = activeRelation;
    }

    public CurrentUserView getCurrentUser() {
        return currentUser;
    }

    public DashboardStatsView getStats() {
        return stats;
    }

    public List<PendingRequestView> getPendingRequests() {
        return pendingRequests;
    }

    public List<ActivityItemView> getActivityItems() {
        return activityItems;
    }

    public ActiveRelationView getActiveRelation() {
        return activeRelation;
    }
}
