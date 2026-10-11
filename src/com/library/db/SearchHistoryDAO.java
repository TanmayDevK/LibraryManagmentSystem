package com.library.db;

import com.library.model.SearchEntry;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Remembers what each member has searched for. */
public class SearchHistoryDAO {

    public void log(int memberId, String keyword) throws SQLException {
        String sql = "INSERT INTO search_history (member_id, keyword, searched_at) VALUES (?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setString(2, keyword);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public List<SearchEntry> getRecent(int memberId, int limit) throws SQLException {
        List<SearchEntry> list = new ArrayList<>();
        String sql = "SELECT keyword, searched_at FROM search_history WHERE member_id=? " +
                "ORDER BY searched_at DESC, id DESC LIMIT ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SearchEntry(rs.getString(1), rs.getTimestamp(2).toLocalDateTime()));
                }
            }
        }
        return list;
    }

    public void clear(int memberId) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("DELETE FROM search_history WHERE member_id=?")) {
            ps.setInt(1, memberId);
            ps.executeUpdate();
        }
    }
}
