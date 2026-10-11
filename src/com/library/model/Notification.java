package com.library.model;

import java.time.LocalDateTime;

public class Notification {
    private final int id;
    private final int memberId;
    private final String type; // ANNOUNCEMENT, DUE_DATE or NEW_ARRIVAL
    private final String message;
    private final LocalDateTime createdAt;
    private final boolean read;

    public Notification(int id, int memberId, String type, String message, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.memberId = memberId;
        this.type = type;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getId() { return id; }
    public int getMemberId() { return memberId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }

    /** Friendly name for the Type column. */
    public String getTypeLabel() {
        switch (type) {
            case "DUE_DATE": return "Due date";
            case "NEW_ARRIVAL": return "New arrival";
            default: return "Announcement";
        }
    }
}
