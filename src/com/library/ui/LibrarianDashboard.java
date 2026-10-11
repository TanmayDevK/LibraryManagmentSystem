package com.library.ui;

import com.library.db.BookDAO;
import com.library.db.CrudDAO;
import com.library.db.NotificationDAO;
import com.library.db.TransactionDAO;
import com.library.db.UserDAO;
import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.LoanRecord;
import com.library.model.User;
import com.library.util.OverdueMonitor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

public class LibrarianDashboard extends JFrame {

    private DefaultTableModel booksModel;
    private DefaultTableModel membersModel;
    private DefaultTableModel transactionsModel;
    private List<LoanRecord> loadedLoans = new ArrayList<>();
    private final DefaultComboBoxModel<Book> issueBookModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<User> issueMemberModel = new DefaultComboBoxModel<>();
    private final NotificationSenderPanel senderPanel = new NotificationSenderPanel();
    private final ReportsPanel reportsPanel = new ReportsPanel();
    private final JLabel overdueLabel = new JLabel("Overdue books: checking...");
    private final OverdueMonitor overdueMonitor;

    public LibrarianDashboard(User librarian) {
        setTitle(librarian.getDashboardTitle()); // polymorphic call
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", buildBooksTab());
        tabs.addTab("Members", buildMembersTab());
        tabs.addTab("Transactions", buildTransactionsTab());
        tabs.addTab("Notifications", senderPanel);
        tabs.addTab("Reports", reportsPanel);
        add(tabs, BorderLayout.CENTER);

        overdueLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(overdueLabel, BorderLayout.SOUTH);

        refreshBooks();
        refreshMembers();
        refreshTransactions();
        reportsPanel.refresh();

        // Background thread: re-checks overdue books every 30 seconds
        overdueMonitor = new OverdueMonitor(overdueLabel::setText);
        overdueMonitor.start(30);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                overdueMonitor.stop();
            }
        });
    }

    // ---------------- Books tab ----------------

    private JPanel buildBooksTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        booksModel = new DefaultTableModel(new Object[]{"ID", "Title", "Author", "ISBN", "Genre", "Total", "Available"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(booksModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 6, 4, 4));
        JTextField title = new JTextField();
        JTextField author = new JTextField();
        JTextField isbn = new JTextField();
        JTextField genre = new JTextField();
        JTextField copies = new JTextField();
        JButton addBtn = new JButton("Add Book");
        form.add(new JLabel("Title")); form.add(new JLabel("Author")); form.add(new JLabel("ISBN"));
        form.add(new JLabel("Genre")); form.add(new JLabel("Total copies")); form.add(new JLabel(""));
        form.add(title); form.add(author); form.add(isbn); form.add(genre); form.add(copies); form.add(addBtn);

        JButton updateBtn = new JButton("Update Selected");
        updateBtn.setToolTipText("Select a book, change the fields above, then click here");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton refreshBtn = new JButton("Refresh");
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(form, BorderLayout.NORTH);
        JPanel actions = new JPanel();
        actions.add(updateBtn);
        actions.add(deleteBtn);
        actions.add(refreshBtn);
        bottom.add(actions, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);

        // Selecting a row copies it into the form so it can be edited
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (e.getValueIsAdjusting() || row == -1) return;
            title.setText(text(booksModel.getValueAt(row, 1)));
            author.setText(text(booksModel.getValueAt(row, 2)));
            isbn.setText(text(booksModel.getValueAt(row, 3)));
            genre.setText(text(booksModel.getValueAt(row, 4)));
            copies.setText(text(booksModel.getValueAt(row, 5)));
        });

        addBtn.addActionListener(e -> {
            try {
                if (title.getText().isBlank() || author.getText().isBlank() || copies.getText().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Title, author and copies are required.");
                    return;
                }
                int copyCount = Integer.parseInt(copies.getText().trim());
                if (copyCount < 1) {
                    JOptionPane.showMessageDialog(this, "Copies must be at least 1.");
                    return;
                }
                String bookTitle = title.getText().trim();
                new BookDAO().addBook(bookTitle, author.getText().trim(),
                        isbn.getText().trim(), genre.getText().trim(), copyCount);
                int notified = new NotificationDAO().broadcastNewArrival(bookTitle);
                title.setText(""); author.setText(""); isbn.setText(""); genre.setText(""); copies.setText("");
                refreshAll();
                JOptionPane.showMessageDialog(this, "Book added successfully."
                        + (notified > 0 ? "\nNew-arrival notification sent to " + notified + " member(s)." : ""));
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Copies must be a number.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a book in the table first.");
                return;
            }
            try {
                if (title.getText().isBlank() || author.getText().isBlank() || copies.getText().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Title, author and copies are required.");
                    return;
                }
                int newTotal = Integer.parseInt(copies.getText().trim());
                int id = (int) booksModel.getValueAt(row, 0);
                int oldTotal = (int) booksModel.getValueAt(row, 5);
                int oldAvailable = (int) booksModel.getValueAt(row, 6);
                int onLoan = oldTotal - oldAvailable;
                if (newTotal < onLoan) {
                    JOptionPane.showMessageDialog(this, onLoan + " copies are currently on loan, so the total can't be below " + onLoan + ".");
                    return;
                }
                new BookDAO().updateBook(id, title.getText().trim(), author.getText().trim(),
                        isbn.getText().trim(), genre.getText().trim(), newTotal);
                refreshAll();
                JOptionPane.showMessageDialog(this, "Book updated successfully.");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Copies must be a number.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a book first.");
                return;
            }
            int id = (int) booksModel.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(this, "Delete this book?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
            try {
                CrudDAO<Book> dao = new BookDAO(); // programmed against the interface
                dao.delete(id);
                refreshAll();
                JOptionPane.showMessageDialog(this, "Book deleted.");
            } catch (SQLIntegrityConstraintViolationException ex) {
                JOptionPane.showMessageDialog(this, "This book can't be deleted because it has loan records.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        refreshBtn.addActionListener(e -> refreshBooks());
        return panel;
    }

    private void refreshBooks() {
        try {
            booksModel.setRowCount(0);
            issueBookModel.removeAllElements();
            CrudDAO<Book> dao = new BookDAO();
            for (Book b : dao.getAll()) {
                booksModel.addRow(new Object[]{b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(),
                        b.getGenre(), b.getTotalCopies(), b.getAvailableCopies()});
                if (b.getAvailableCopies() > 0) {
                    issueBookModel.addElement(b); // only books that can be issued
                }
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ---------------- Members tab ----------------

    private JPanel buildMembersTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        membersModel = new DefaultTableModel(new Object[]{"Membership ID", "Username", "Full Name", "Email"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(membersModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton addBtn = new JButton("Add Member");

        JPanel form = new JPanel(new GridLayout(2, 5, 4, 4));
        form.add(new JLabel("Full Name")); form.add(new JLabel("Email")); form.add(new JLabel("Username"));
        form.add(new JLabel("Password")); form.add(new JLabel(""));
        form.add(nameField); form.add(emailField); form.add(usernameField); form.add(passwordField); form.add(addBtn);

        JButton updateBtn = new JButton("Update Selected (name, email, new password)");
        JButton deleteBtn = new JButton("Remove Selected Member");
        JButton clearBtn = new JButton("Clear Form");
        JButton refreshBtn = new JButton("Refresh");
        JPanel actions = new JPanel();
        actions.add(updateBtn);
        actions.add(deleteBtn);
        actions.add(clearBtn);
        actions.add(refreshBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(form, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (e.getValueIsAdjusting() || row == -1) return;
            usernameField.setText(text(membersModel.getValueAt(row, 1)));
            nameField.setText(text(membersModel.getValueAt(row, 2)));
            emailField.setText(text(membersModel.getValueAt(row, 3)));
            passwordField.setText("");
        });

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name, username and password are required to create a member.");
                return;
            }
            try {
                boolean created = new UserDAO().registerMember(username, password, name, emailField.getText().trim());
                if (!created) {
                    JOptionPane.showMessageDialog(this, "That username is already taken.");
                    return;
                }
                clearMemberForm(nameField, emailField, usernameField, passwordField);
                refreshMembers();
                JOptionPane.showMessageDialog(this, "Member account created successfully.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a member in the table first.");
                return;
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name can't be empty.");
                return;
            }
            int id = (int) membersModel.getValueAt(row, 0);
            String password = new String(passwordField.getPassword());
            try {
                UserDAO dao = new UserDAO();
                if (password.isEmpty()) {
                    dao.updateProfile(id, name, emailField.getText().trim());
                } else {
                    dao.updateProfile(id, name, emailField.getText().trim(), password);
                }
                passwordField.setText("");
                refreshMembers();
                JOptionPane.showMessageDialog(this, "Member updated successfully.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a member first.");
                return;
            }
            int id = (int) membersModel.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(this, "Remove this member?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
            try {
                new UserDAO().deleteMember(id);
                refreshMembers();
                JOptionPane.showMessageDialog(this, "Member removed.");
            } catch (SQLIntegrityConstraintViolationException ex) {
                JOptionPane.showMessageDialog(this, "This member can't be removed because they have loan records.");
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        clearBtn.addActionListener(e -> clearMemberForm(nameField, emailField, usernameField, passwordField));
        refreshBtn.addActionListener(e -> refreshMembers());
        return panel;
    }

    private void clearMemberForm(JTextField name, JTextField email, JTextField username, JPasswordField password) {
        name.setText("");
        email.setText("");
        username.setText("");
        password.setText("");
    }

    private void refreshMembers() {
        try {
            membersModel.setRowCount(0);
            issueMemberModel.removeAllElements();
            for (User u : new UserDAO().getAllMembers()) {
                membersModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getFullName(), u.getEmail()});
                issueMemberModel.addElement(u);
            }
            senderPanel.reload();
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ---------------- Transactions tab ----------------

    private JPanel buildTransactionsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        transactionsModel = new DefaultTableModel(
                new Object[]{"ID", "Book", "Member", "Borrow Date", "Due Date", "Return Date", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(transactionsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JComboBox<Book> bookCombo = new JComboBox<>(issueBookModel);
        bookCombo.setPreferredSize(new Dimension(280, 26));
        JComboBox<User> memberCombo = new JComboBox<>(issueMemberModel);
        memberCombo.setPreferredSize(new Dimension(240, 26));
        JButton issueBtn = new JButton("Issue Book");

        JPanel issueRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        issueRow.add(new JLabel("Book:"));
        issueRow.add(bookCombo);
        issueRow.add(new JLabel("Member:"));
        issueRow.add(memberCombo);
        issueRow.add(issueBtn);

        JButton returnBtn = new JButton("Mark Selected as Returned");
        JButton refreshBtn = new JButton("Refresh");
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionRow.add(returnBtn);
        actionRow.add(refreshBtn);

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.add(issueRow);
        bottom.add(actionRow);
        panel.add(bottom, BorderLayout.SOUTH);

        issueBtn.addActionListener(e -> {
            Book book = (Book) bookCombo.getSelectedItem();
            User member = (User) memberCombo.getSelectedItem();
            if (book == null || member == null) {
                JOptionPane.showMessageDialog(this, "Choose a book and a member first.");
                return;
            }
            try {
                new TransactionDAO().borrowBook(book.getId(), member.getId(), member.getBorrowLimit());
                refreshAll();
                JOptionPane.showMessageDialog(this, "Book issued to " + member.getFullName() + ". Due in 14 days.");
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Can't issue", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        returnBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a transaction first.");
                return;
            }
            LoanRecord record = loadedLoans.get(table.convertRowIndexToModel(row));
            if ("RETURNED".equals(record.getStatus())) {
                JOptionPane.showMessageDialog(this, "That book was already returned.");
                return;
            }
            try {
                new TransactionDAO().returnBook(record.getId(), record.getBookId());
                refreshAll();
                JOptionPane.showMessageDialog(this, "Book marked as returned.");
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            } catch (SQLException ex) {
                showDbError(ex);
            }
        });

        refreshBtn.addActionListener(e -> refreshTransactions());
        return panel;
    }

    private void refreshTransactions() {
        try {
            transactionsModel.setRowCount(0);
            loadedLoans = new TransactionDAO().getAllTransactions();
            for (LoanRecord r : loadedLoans) {
                transactionsModel.addRow(new Object[]{r.getId(), r.getBookTitle(), r.getMemberName(),
                        r.getBorrowDate(), r.getDueDate(),
                        r.getReturnDate() == null ? "-" : r.getReturnDate(), r.getDisplayStatus()});
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ---------------- helpers ----------------

    /** Refresh everything that depends on books or loans. */
    private void refreshAll() {
        refreshBooks();
        refreshTransactions();
        reportsPanel.refresh();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
