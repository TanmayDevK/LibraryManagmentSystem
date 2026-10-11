package com.library.ui;

import com.library.db.NotificationDAO;
import com.library.db.UserDAO;
import com.library.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Librarian tool: send an announcement to all members or to chosen members. */
public class NotificationSenderPanel extends JPanel {

    private final DefaultListModel<User> memberModel = new DefaultListModel<>();
    private final JList<User> memberList = new JList<>(memberModel);
    private final JRadioButton allRadio = new JRadioButton("All members", true);
    private final JRadioButton selectedRadio = new JRadioButton("Selected members");
    private final JTextArea messageArea = new JTextArea(5, 30);
    private static final String[] TYPE_NAMES = {"General announcement", "Due-date reminder", "New arrival"};
    private static final String[] TYPE_KEYS = {"ANNOUNCEMENT", "DUE_DATE", "NEW_ARRIVAL"};
    private final JComboBox<String> typeBox = new JComboBox<>(TYPE_NAMES);

    public NotificationSenderPanel() {
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Type:"));
        top.add(typeBox);
        top.add(new JLabel("   Send to:"));
        ButtonGroup group = new ButtonGroup();
        group.add(allRadio);
        group.add(selectedRadio);
        top.add(allRadio);
        top.add(selectedRadio);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));

        JScrollPane listScroll = new JScrollPane(memberList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Recipients (hold Ctrl to pick several)"));
        center.add(listScroll);

        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createTitledBorder("Message"));
        center.add(messageScroll);
        add(center, BorderLayout.CENTER);

        JButton sendBtn = new JButton("Send Notification");
        JPanel bottom = new JPanel();
        bottom.add(sendBtn);
        add(bottom, BorderLayout.SOUTH);

        memberList.setEnabled(false);
        allRadio.addActionListener(e -> memberList.setEnabled(false));
        selectedRadio.addActionListener(e -> memberList.setEnabled(true));
        sendBtn.addActionListener(e -> send());

        reload();
    }

    /** Reloads the member list (call after members are added or removed). */
    public void reload() {
        try {
            memberModel.clear();
            for (User u : new UserDAO().getAllMembers()) {
                memberModel.addElement(u);
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void send() {
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Type a message first.");
            return;
        }
        List<Integer> ids = null; // null = everyone
        int targeted = memberModel.size();
        if (selectedRadio.isSelected()) {
            List<User> chosen = memberList.getSelectedValuesList();
            if (chosen.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select at least one member.");
                return;
            }
            ids = new ArrayList<>();
            for (User u : chosen) {
                ids.add(u.getId());
            }
            targeted = ids.size();
        }
        if (targeted == 0) {
            JOptionPane.showMessageDialog(this, "There are no members to notify yet.");
            return;
        }
        try {
            int delivered = new NotificationDAO().sendNotification(TYPE_KEYS[typeBox.getSelectedIndex()], message, ids);
            String result = "Notification sent to " + delivered + " of " + targeted + " member(s).";
            if (delivered < targeted) {
                result += "\n" + (targeted - delivered) + " member(s) have turned this type of alert off.";
            }
            JOptionPane.showMessageDialog(this, result);
            messageArea.setText("");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
