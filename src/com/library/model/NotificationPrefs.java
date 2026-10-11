package com.library.model;

/** Which kinds of notification a member wants to receive. */
public class NotificationPrefs {
    private final boolean dueAlerts;
    private final boolean newArrivalAlerts;
    private final boolean announcements;

    public NotificationPrefs(boolean dueAlerts, boolean newArrivalAlerts, boolean announcements) {
        this.dueAlerts = dueAlerts;
        this.newArrivalAlerts = newArrivalAlerts;
        this.announcements = announcements;
    }

    public static NotificationPrefs defaults() {
        return new NotificationPrefs(true, true, true);
    }

    public boolean isDueAlerts() { return dueAlerts; }
    public boolean isNewArrivalAlerts() { return newArrivalAlerts; }
    public boolean isAnnouncements() { return announcements; }
}
