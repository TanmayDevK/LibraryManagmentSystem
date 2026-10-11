package com.library.util;

import com.library.db.TransactionDAO;

import javax.swing.SwingUtilities;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background thread that periodically counts overdue books and reports the
 * result back to the GUI. The UI update is handed to the Swing thread with
 * SwingUtilities.invokeLater, because Swing components must only be touched
 * from the event dispatch thread.
 */
public class OverdueMonitor {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "overdue-monitor");
        t.setDaemon(true); // never keeps the app alive on its own
        return t;
    });
    private final Consumer<String> onUpdate;

    public OverdueMonitor(Consumer<String> onUpdate) {
        this.onUpdate = onUpdate;
    }

    public void start(long periodSeconds) {
        scheduler.scheduleAtFixedRate(this::check, 0, periodSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void check() {
        String message;
        try {
            int overdue = new TransactionDAO().countOverdue();
            message = "Overdue books: " + overdue + "   (checked at " + LocalTime.now().withNano(0) + ")";
        } catch (Exception e) {
            message = "Overdue check failed: " + e.getMessage();
        }
        final String text = message;
        SwingUtilities.invokeLater(() -> onUpdate.accept(text));
    }
}
