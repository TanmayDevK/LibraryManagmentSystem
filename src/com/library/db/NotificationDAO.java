package com.library.db;

import com.library.model.Notification;
import com.library.model.NotificationPrefs;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Notifications sent to members, plus each member's notification preferences. */
public class NotificationDAO {

    /**
     * Sends a notification of the given type (ANNOUNCEMENT, DUE_DATE or NEW_ARRIVAL).
     * memberIds == null (or empty) means "all members". Members who switched that
     * type of alert off are skipped. Returns how many members actually received it.
     */
    public int sendNotification(String type, String message, List<Integer> memberIds) throws SQLException {
        String prefColumn;
        switch (type) { // fixed whitelist, so it is safe to put into the SQL text
            case "DUE_DATE": prefColumn = "p.due_alerts"; break;
            case "NEW_ARRIVAL": prefColumn = "p.new_arrival_alerts"; break;
            case "ANNOUNCEMENT": prefColumn = "p.announcements"; break;
            default: throw new IllegalArgumentException("Unknown notification type: " + type);
        }
        boolean selected = memberIds != null && !memberIds.isEmpty();
        StringBuilder sql = new StringBuilder(
                "INSERT INTO notifications (member_id, type, message, created_at) " +
                "SELECT u.id, '" + type + "', ?, ? FROM users u " +
                "LEFT JOIN notification_prefs p ON p.member_id = u.id " +
                "WHERE u.role='MEMBER' AND COALESCE(" + prefColumn + ", TRUE)");
        if (selected) {
            sql.append(" AND u.id IN (");
            for (int i = 0; i < memberIds.size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
            }
            sql.append(")");
        }
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            ps.setString(1, message);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            if (selected) {
                for (int i = 0; i < memberIds.size(); i++) {
                    ps.setInt(3 + i, memberIds.get(i));
                }
            }
            return ps.executeUpdate();
        }
    }

    /** Shortcut for a general announcement. */
    public int sendAnnouncement(String message, List<Integer> memberIds) throws SQLException {
        return sendNotification("ANNOUNCEMENT", message, memberIds);
    }

    /** Tells every member (who wants new-arrival alerts) that a book was added. */
    public int broadcastNewArrival(String bookTitle) throws SQLException {
        String sql = "INSERT INTO notifications (member_id, type, message, created_at) " +
                "SELECT u.id, 'NEW_ARRIVAL', ?, ? FROM users u " +
                "LEFT JOIN notification_prefs p ON p.member_id = u.id " +
                "WHERE u.role='MEMBER' AND COALESCE(p.new_arrival_alerts, TRUE)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, "New arrival: '" + bookTitle + "' is now available in the library.");
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate();
        }
    }

    public List<Notification> getForMember(int memberId) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, member_id, type, message, created_at, is_read FROM notifications " +
                "WHERE member_id=? ORDER BY created_at DESC, id DESC LIMIT 100";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                            rs.getInt("id"),
                            rs.getInt("member_id"),
                            rs.getString("type"),
                            rs.getString("message"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getBoolean("is_read")));
                }
            }
        }
        return list;
    }

    /** Called from a background thread, so it takes the shared lock. */
    public int countUnread(int memberId) throws SQLException {
        synchronized (DbLock.INSTANCE) {
            String sql = "SELECT COUNT(*) FROM notifications WHERE member_id=? AND is_read=FALSE";
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, memberId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        }
    }

    public void markAllRead(int memberId) throws SQLException {
        String sql = "UPDATE notifications SET is_read=TRUE WHERE member_id=? AND is_read=FALSE";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }

    // ---------------- preferences ----------------

    public NotificationPrefs getPrefs(int memberId) throws SQLException {
        String sql = "SELECT due_alerts, new_arrival_alerts, announcements FROM notification_prefs WHERE member_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new NotificationPrefs(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3));
                }
            }
        }
        return NotificationPrefs.defaults(); // never saved -> everything on
    }

    public void savePrefs(int memberId, NotificationPrefs prefs) throws SQLException {
        String sql = "INSERT INTO notification_prefs (member_id, due_alerts, new_arrival_alerts, announcements) " +
                "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE due_alerts=VALUES(due_alerts), " +
                "new_arrival_alerts=VALUES(new_arrival_alerts), announcements=VALUES(announcements)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setBoolean(2, prefs.isDueAlerts());
            ps.setBoolean(3, prefs.isNewArrivalAlerts());
            ps.setBoolean(4, prefs.isAnnouncements());
            ps.executeUpdate();
        }
    }

    // ---------------- automatic due-date alerts ----------------

    /**
     * Creates reminders for this member's books that are overdue, due today, or due
     * within 2 days. At most one identical reminder per day. Runs on a background
     * thread, so it takes the shared lock. Returns how many new alerts were created.
     */
    public int generateDueAlerts(int memberId) throws SQLException {
        synchronized (DbLock.INSTANCE) {
            if (!getPrefs(memberId).isDueAlerts()) {
                return 0;
            }
            Connection conn = DBConnection.getConnection();
            LocalDate today = LocalDate.now();

            List<String> messages = new ArrayList<>();
            String loans = "SELECT b.title, t.due_date FROM transactions t JOIN books b ON t.book_id=b.id " +
                    "WHERE t.member_id=? AND t.status='BORROWED' AND t.due_date <= ?";
            try (PreparedStatement ps = conn.prepareStatement(loans)) {
                ps.setInt(1, memberId);
                ps.setDate(2, Date.valueOf(today.plusDays(2)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString(1);
                        LocalDate due = rs.getDate(2).toLocalDate();
                        if (due.isBefore(today)) {
                            messages.add("OVERDUE: '" + title + "' was due on " + due + ". Please return it.");
                        } else if (due.isEqual(today)) {
                            messages.add("Due today: '" + title + "'. Please return it.");
                        } else {
                            messages.add("Reminder: '" + title + "' is due on " + due + ".");
                        }
                    }
                }
            }

            Timestamp startOfToday = Timestamp.valueOf(today.atStartOfDay());
            int created = 0;
            for (String message : messages) {
                boolean alreadySent;
                String check = "SELECT 1 FROM notifications WHERE member_id=? AND type='DUE_DATE' " +
                        "AND message=? AND created_at >= ?";
                try (PreparedStatement ps = conn.prepareStatement(check)) {
                    ps.setInt(1, memberId);
                    ps.setString(2, message);
                    ps.setTimestamp(3, startOfToday);
                    try (ResultSet rs = ps.executeQuery()) {
                        alreadySent = rs.next();
                    }
                }
                if (!alreadySent) {
                    String insert = "INSERT INTO notifications (member_id, type, message, created_at) " +
                            "VALUES (?, 'DUE_DATE', ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insert)) {
                        ps.setInt(1, memberId);
                        ps.setString(2, message);
                        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        ps.executeUpdate();
                        created++;
                    }
                }
            }
            return created;
        }
    }
}
