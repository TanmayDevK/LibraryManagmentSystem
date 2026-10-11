package com.library.util;

import com.library.db.NotificationDAO;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background thread for a logged-in member. Every few seconds it creates any
 * due-date alerts that are needed, counts unread notifications, and hands the
 * number to the GUI (on the Swing thread) so alerts appear without clicking Refresh.
 */
public class NotificationMonitor {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "notification-monitor");
        t.setDaemon(true);
        return t;
    });
    private final int memberId;
    private final Consumer<Integer> onUnreadCount;

    public NotificationMonitor(int memberId, Consumer<Integer> onUnreadCount) {
        this.memberId = memberId;
        this.onUnreadCount = onUnreadCount;
    }

    public void start(long periodSeconds) {
        scheduler.scheduleAtFixedRate(this::check, 0, periodSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void check() {
        try {
            NotificationDAO dao = new NotificationDAO();
            dao.generateDueAlerts(memberId);
            int unread = dao.countUnread(memberId);
            SwingUtilities.invokeLater(() -> onUnreadCount.accept(unread));
        } catch (Exception e) {
            // Database hiccup: just try again on the next round.
        }
    }
}
