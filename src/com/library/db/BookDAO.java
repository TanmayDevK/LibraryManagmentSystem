package com.library.db;

import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Book table access. Implements two interfaces: CrudDAO and Searchable. */
public class BookDAO implements CrudDAO<Book>, Searchable<Book> {

    public List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        }
        return books;
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR genre LIKE ? ORDER BY title";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            }
        }
        return books;
    }

    public void addBook(String title, String author, String isbn, String genre, int copies) throws SQLException {
        String sql = "INSERT INTO books (title, author, isbn, genre, total_copies, available_copies) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, isbn);
            ps.setString(4, genre);
            ps.setInt(5, copies);
            ps.setInt(6, copies);
            ps.executeUpdate();
        }
    }

    public void updateBook(int id, String title, String author, String isbn, String genre, int totalCopies) throws SQLException {
        // Adjust available_copies by the same delta as total_copies changes
        String sql = "UPDATE books SET title=?, author=?, isbn=?, genre=?, " +
                "available_copies = available_copies + (? - total_copies), total_copies=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, isbn);
            ps.setString(4, genre);
            ps.setInt(5, totalCopies);
            ps.setInt(6, totalCopies);
            ps.setInt(7, id);
            ps.executeUpdate();
        }
    }

    public void deleteBook(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ----- interface implementations -----

    @Override
    public List<Book> getAll() throws SQLException {
        return getAllBooks();
    }

    @Override
    public void delete(int id) throws SQLException {
        deleteBook(id);
    }

    @Override
    public List<Book> search(String keyword) throws SQLException {
        return searchBooks(keyword);
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("isbn"),
                rs.getString("genre"),
                rs.getInt("total_copies"),
                rs.getInt("available_copies")
        );
    }
}
