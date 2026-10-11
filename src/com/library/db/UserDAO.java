package com.library.db;

import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /** Returns the matching User (a Librarian or a Member), or null if credentials are wrong. */
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /** Registers a new member account. Returns false if the username is taken. */
    public boolean registerMember(String username, String password, String fullName, String email) throws SQLException {
        String check = "SELECT id FROM users WHERE username=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(check)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return false;
            }
        }
        String sql = "INSERT INTO users (username, password, full_name, email, role) VALUES (?,?,?,?,'MEMBER')";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.executeUpdate();
        }
        return true;
    }

    public List<User> getAllMembers() throws SQLException {
        List<User> members = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role='MEMBER' ORDER BY full_name";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
        }
        return members;
    }

    public void deleteMember(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id=? AND role='MEMBER'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Updates name/email only, leaving the password untouched. */
    public void updateProfile(int id, String fullName, String email) throws SQLException {
        String sql = "UPDATE users SET full_name=?, email=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /** Updates name/email and sets a new password. */
    public void updateProfile(int id, String fullName, String email, String newPassword) throws SQLException {
        String sql = "UPDATE users SET full_name=?, email=?, password=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, newPassword);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    /** Builds the right subclass depending on the role column. */
    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        if ("LIBRARIAN".equals(rs.getString("role"))) {
            return new Librarian(id, username, fullName, email);
        }
        return new Member(id, username, fullName, email);
    }
}
