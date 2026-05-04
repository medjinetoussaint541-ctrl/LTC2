package com.team.apk.Dto;

public class ActivityItemView {

    private final String title;
    private final String subtitle;
    private final String timeLabel;
    private final String icon;
    private final String accent;
    private final long sortKey;

    public ActivityItemView(String title, String subtitle, String timeLabel, String icon, String accent, long sortKey) {
        this.title = title;
        this.subtitle = subtitle;
        this.timeLabel = timeLabel;
        this.icon = icon;
        this.accent = accent;
        this.sortKey = sortKey;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public String getIcon() {
        return icon;
    }

    public String getAccent() {
        return accent;
    }

    public long getSortKey() {
        return sortKey;
    }
}
