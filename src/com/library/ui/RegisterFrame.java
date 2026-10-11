package com.library.ui;

import com.library.db.UserDAO;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class RegisterFrame extends JFrame {

    private final JTextField nameField = new JTextField(16);
    private final JTextField emailField = new JTextField(16);
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final LoginFrame parent;

    public RegisterFrame(LoginFrame parent) {
        this.parent = parent;
        setTitle("Register as Member");
        setSize(380, 320);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, gbc, 0, "Full Name:", nameField);
        addRow(panel, gbc, 1, "Email:", emailField);
        addRow(panel, gbc, 2, "Username:", usernameField);
        addRow(panel, gbc, 3, "Password:", passwordField);

        JButton registerBtn = new JButton("Create Account");
        JButton backBtn = new JButton("Back to Login");

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(registerBtn, gbc);
        gbc.gridy = 5;
        panel.add(backBtn, gbc);

        add(panel);

        registerBtn.addActionListener(e -> doRegister());
        backBtn.addActionListener(e -> {
            dispose();
            parent.setVisible(true);
        });
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void doRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, username and password are required.");
            return;
        }

        try {
            boolean ok = new UserDAO().registerMember(username, password, name, email);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "That username is already taken.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Account created! You can now log in.");
            dispose();
            parent.setVisible(true);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to the database:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
