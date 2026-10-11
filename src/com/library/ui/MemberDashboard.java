package com.library.ui;

import com.library.db.BookDAO;
import com.library.db.SearchHistoryDAO;
import com.library.db.Searchable;
import com.library.db.TransactionDAO;
import com.library.db.UserDAO;
import com.library.exception.BookNotAvailableException;
import com.library.exception.BorrowLimitExceededException;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LoanRecord;
import com.library.model.User;
import com.library.util.NotificationMonitor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

public class MemberDashboard extends JFrame {

    private static final int NOTIFICATIONS_TAB = 3;

    private final User member;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = new JLabel("No new notifications.");
    private DefaultTableModel searchModel;
    private DefaultTableModel historyModel;
    private final SearchHistoryPanel searchHistoryPanel;
    private final MemberNotificationsPanel notificationsPanel;
    private final NotificationMonitor notificationMonitor;
    private int lastUnread = -1;

    public MemberDashboard(User member) {
        this.member = member;
        setTitle(member.getDashboardTitle()); // polymorphic call
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 580);
        setLocationRelativeTo(null);

        searchHistoryPanel = new SearchHistoryPanel(member, this::searchAgain);
        notificationsPanel = new MemberNotificationsPanel(member, this::onUnreadChanged);

        tabs.addTab("Search & Borrow", buildSearchTab());
        tabs.addTab("My Borrowing History", buildHistoryTab());
        tabs.addTab("Search History", searchHistoryPanel);
        tabs.addTab("Notifications", notificationsPanel);
        tabs.addTab("Profile", buildProfileTab());
        add(tabs, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);

        refreshSearch("");
        refreshHistory();
        notificationsPanel.refresh();

        // Background thread: creates due-date alerts and checks for new notifications every 15 seconds
        notificationMonitor = new NotificationMonitor(member.getId(), this::onMonitorUpdate);
        notificationMonitor.start(15);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                notificationMonitor.stop();
            }
        });
    }

    // ---------------- Notification badge ----------------

    /** Called by the notifications panel after it (re)loads. */
    private void onUnreadChanged(int unread) {
        lastUnread = unread;
        updateBadge(unread);
    }

    /** Called by the background monitor (already on the Swing thread). */
    private void onMonitorUpdate(int unread) {
        if (unread != lastUnread) {
            notificationsPanel.refresh(); // new alerts arrived -> reload the inbox
        } else {
            updateBadge(unread);
        }
    }

    private void updateBadge(int unread) {
        tabs.setTitleAt(NOTIFICATIONS_TAB, unread > 0 ? "Notifications (" + unread + " new)" : "Notifications");
        statusLabel.setText(unread > 0
                ? "You have " + unread + " unread notification(s) - see the Notifications tab."
                : "No new notifications.");
    }

    // ---------------- Search & Borrow tab ----------------

    private JPanel buildSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout(4, 4));
        JButton searchBtn = new JButton("Search");
        top.add(searchField, BorderLayout.CENTER);
        top.add(searchBtn, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        searchModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "Genre", "Available"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(searchModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton borrowBtn = new JButton("Borrow Selected Book (14-day loan, max "
                + member.getBorrowLimit() + " at a time)");
        JPanel actions = new JPanel();
        actions.add(borrowBtn);
        panel.add(actions, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> doSearch(searchField.getText().trim()));
        searchField.addActionListener(e -> doSearch(searchField.getText().trim()));

        borrowBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a book first.");
                return;
            }
            int bookId = (int) searchModel.getValueAt(row, 0);
            try {
                new TransactionDAO().borrowBook(bookId, member.getId(), member.getBorrowLimit());
                JOptionPane.showMessageDialog(this, "Book borrowed! Due back in 14 days.");
                refreshSearch(searchField.getText().trim());
                refreshHistory();
            } catch (BookNotAvailableException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Not Available", JOptionPane.WARNING_MESSAGE);
                refreshSearch(searchField.getText().trim());
            } catch (BorrowLimitExceededException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Limit Reached", JOptionPane.WARNING_MESSAGE);
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        return panel;
    }

    /** A search the member typed: remember it, then show the results. */
    private void doSearch(String keyword) {
        if (!keyword.isEmpty()) {
            try {
                new SearchHistoryDAO().log(member.getId(), keyword);
            } catch (SQLException ex) {
                showDbError(ex);
            }
            searchHistoryPanel.refresh();
        }
        refreshSearch(keyword);
    }

    /** "Search Again" button on the Search History tab. */
    private void searchAgain(String keyword) {
        searchField.setText(keyword);
        tabs.setSelectedIndex(0);
        doSearch(keyword);
    }

    private void refreshSearch(String keyword) {
        try {
            searchModel.setRowCount(0);
            BookDAO dao = new BookDAO();
            Searchable<Book> searcher = dao; // used through the Searchable interface
            List<Book> books = keyword.isEmpty() ? dao.getAll() : searcher.search(keyword);
            for (Book b : books) {
                searchModel.addRow(new Object[]{b.getId(), b.getTitle(), b.getAuthor(), b.getGenre(), b.getAvailableCopies()});
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ---------------- History tab ----------------

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        historyModel = new DefaultTableModel(
                new Object[]{"Txn ID", "Book", "Borrow Date", "Due Date", "Return Date", "Status", "BookId"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(historyModel);
        table.removeColumn(table.getColumnModel().getColumn(6)); // hide raw bookId helper column
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton returnBtn = new JButton("Return Selected Book");
        JButton refreshBtn = new JButton("Refresh");
        JPanel actions = new JPanel();
        actions.add(returnBtn);
        actions.add(refreshBtn);
        panel.add(actions, BorderLayout.SOUTH);

        returnBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a transaction first.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            String status = (String) historyModel.getValueAt(modelRow, 5);
            if ("RETURNED".equals(status)) {
                JOptionPane.showMessageDialog(this, "That book was already returned.");
                return;
            }
            int txnId = (int) historyModel.getValueAt(modelRow, 0);
            int bookId = (int) historyModel.getValueAt(modelRow, 6);
            try {
                new TransactionDAO().returnBook(txnId, bookId);
                JOptionPane.showMessageDialog(this, "Book returned. Thanks!");
                refreshHistory();
                refreshSearch(searchField.getText().trim());
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        refreshBtn.addActionListener(e -> refreshHistory());
        return panel;
    }

    private void refreshHistory() {
        try {
            historyModel.setRowCount(0);
            for (LoanRecord r : new TransactionDAO().getTransactionsForMember(member.getId())) {
                historyModel.addRow(new Object[]{r.getId(), r.getBookTitle(), r.getBorrowDate(), r.getDueDate(),
                        r.getReturnDate() == null ? "-" : r.getReturnDate(), r.getDisplayStatus(), r.getBookId()});
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ---------------- Profile tab ----------------

    private JPanel buildProfileTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(member.getFullName(), 20);
        JTextField emailField = new JTextField(member.getEmail(), 20);
        JPasswordField passwordField = new JPasswordField(20);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        JButton saveBtn = new JButton("Save Changes");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(saveBtn, gbc);

        saveBtn.addActionListener(e -> {
            String password = new String(passwordField.getPassword());
            try {
                UserDAO dao = new UserDAO();
                if (password.isEmpty()) {
                    dao.updateProfile(member.getId(), nameField.getText().trim(), emailField.getText().trim());
                } else {
                    dao.updateProfile(member.getId(), nameField.getText().trim(), emailField.getText().trim(), password);
                }
                JOptionPane.showMessageDialog(this, "Profile updated.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        return panel;
    }

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
