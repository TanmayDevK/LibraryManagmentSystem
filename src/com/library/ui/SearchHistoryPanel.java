package com.library.ui;

import com.library.db.SearchHistoryDAO;
import com.library.model.SearchEntry;
import com.library.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Table of what this member has searched for before. */
public class SearchHistoryPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final User member;
    private final DefaultTableModel model;

    public SearchHistoryPanel(User member, Consumer<String> onSearchAgain) {
        this.member = member;
        setLayout(new BorderLayout(8, 8));

        model = new DefaultTableModel(new Object[]{"Searched for", "When"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton againBtn = new JButton("Search Again");
        JButton clearBtn = new JButton("Clear History");
        JButton refreshBtn = new JButton("Refresh");
        JPanel actions = new JPanel();
        actions.add(againBtn);
        actions.add(clearBtn);
        actions.add(refreshBtn);
        add(actions, BorderLayout.SOUTH);

        againBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a past search first.");
                return;
            }
            onSearchAgain.accept((String) model.getValueAt(row, 0));
        });
        clearBtn.addActionListener(e -> {
            try {
                new SearchHistoryDAO().clear(member.getId());
                refresh();
            } catch (SQLException ex) {
                showError(ex);
            }
        });
        refreshBtn.addActionListener(e -> refresh());

        refresh();
    }

    public void refresh() {
        try {
            model.setRowCount(0);
            for (SearchEntry s : new SearchHistoryDAO().getRecent(member.getId(), 50)) {
                model.addRow(new Object[]{s.getKeyword(), s.getSearchedAt().format(TIME_FORMAT)});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
