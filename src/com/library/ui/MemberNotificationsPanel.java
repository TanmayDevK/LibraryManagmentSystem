package com.library.ui;

import com.library.db.NotificationDAO;
import com.library.model.Notification;
import com.library.model.NotificationPrefs;
import com.library.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.function.IntConsumer;

/** Member's inbox of notifications plus the preference checkboxes. */
public class MemberNotificationsPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final User member;
    private final IntConsumer onUnreadChanged;
    private final DefaultTableModel model;
    private final JCheckBox dueBox = new JCheckBox("Due-date reminders");
    private final JCheckBox arrivalBox = new JCheckBox("New arrivals");
    private final JCheckBox announceBox = new JCheckBox("Librarian announcements");

    public MemberNotificationsPanel(User member, IntConsumer onUnreadChanged) {
        this.member = member;
        this.onUnreadChanged = onUnreadChanged;
        setLayout(new BorderLayout(8, 8));

        model = new DefaultTableModel(new Object[]{"Type", "Message", "Received", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton markReadBtn = new JButton("Mark All as Read");
        JButton refreshBtn = new JButton("Refresh");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(markReadBtn);
        buttons.add(refreshBtn);

        JButton saveBtn = new JButton("Save Preferences");
        JPanel prefs = new JPanel(new FlowLayout(FlowLayout.LEFT));
        prefs.setBorder(BorderFactory.createTitledBorder("Notification preferences"));
        prefs.add(dueBox);
        prefs.add(arrivalBox);
        prefs.add(announceBox);
        prefs.add(saveBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(prefs, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        markReadBtn.addActionListener(e -> markAllRead());
        refreshBtn.addActionListener(e -> refresh());
        saveBtn.addActionListener(e -> savePrefs());

        loadPrefs();
    }

    /** Reloads the inbox and reports the unread count to the dashboard. */
    public void refresh() {
        try {
            model.setRowCount(0);
            int unread = 0;
            for (Notification n : new NotificationDAO().getForMember(member.getId())) {
                if (!n.isRead()) unread++;
                model.addRow(new Object[]{n.getTypeLabel(), n.getMessage(),
                        n.getCreatedAt().format(TIME_FORMAT), n.isRead() ? "Read" : "NEW"});
            }
            onUnreadChanged.accept(unread);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void markAllRead() {
        try {
            new NotificationDAO().markAllRead(member.getId());
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadPrefs() {
        try {
            NotificationPrefs p = new NotificationDAO().getPrefs(member.getId());
            dueBox.setSelected(p.isDueAlerts());
            arrivalBox.setSelected(p.isNewArrivalAlerts());
            announceBox.setSelected(p.isAnnouncements());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void savePrefs() {
        try {
            new NotificationDAO().savePrefs(member.getId(),
                    new NotificationPrefs(dueBox.isSelected(), arrivalBox.isSelected(), announceBox.isSelected()));
            JOptionPane.showMessageDialog(this, "Notification preferences saved.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
