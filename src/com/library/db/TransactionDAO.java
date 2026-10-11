package com.library.db;

import com.library.exception.BookNotAvailableException;
import com.library.exception.BorrowLimitExceededException;
import com.library.exception.LibraryException;
import com.library.model.LoanRecord;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Borrow / return logic.
 *
 * Transaction management: borrowBook and returnBook turn auto-commit off, run
 * several statements, then commit - or roll everything back if anything fails.
 *
 * Synchronization: every method locks on DbLock.INSTANCE, so the background overdue
 * monitor thread and the Swing thread never use the shared connection in the
 * middle of a transaction, and two borrowers can't both take the last copy.
 */
public class TransactionDAO {

    private static final int LOAN_DAYS = 14;

    private static final String SELECT_LOANS =
            "SELECT t.id, t.book_id, b.title, u.full_name, t.borrow_date, t.due_date, t.return_date, t.status " +
            "FROM transactions t JOIN books b ON t.book_id=b.id JOIN users u ON t.member_id=u.id ";

    /** Borrows a book atomically: limit check + copy decrement + loan insert, all or nothing. */
    public void borrowBook(int bookId, int memberId, int borrowLimit) throws SQLException, LibraryException {
        synchronized (DbLock.INSTANCE) {
            Connection conn = DBConnection.getConnection();
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (countActiveLoans(conn, memberId) >= borrowLimit) {
                    throw new BorrowLimitExceededException(
                            "You already have " + borrowLimit + " books out. Return one before borrowing another.");
                }

                // Only succeeds if a copy is still available (no race between two users)
                String take = "UPDATE books SET available_copies = available_copies - 1 WHERE id=? AND available_copies > 0";
                try (PreparedStatement ps = conn.prepareStatement(take)) {
                    ps.setInt(1, bookId);
                    if (ps.executeUpdate() == 0) {
                        throw new BookNotAvailableException("No copies of this book are available right now.");
                    }
                }

                String insert = "INSERT INTO transactions (book_id, member_id, borrow_date, due_date, status) " +
                        "VALUES (?, ?, ?, ?, 'BORROWED')";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setDate(3, Date.valueOf(LocalDate.now()));
                    ps.setDate(4, Date.valueOf(LocalDate.now().plusDays(LOAN_DAYS)));
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /** Returns a book atomically: mark the loan returned + give the copy back. */
    public void returnBook(int transactionId, int bookId) throws SQLException, LibraryException {
        synchronized (DbLock.INSTANCE) {
            Connection conn = DBConnection.getConnection();
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                String close = "UPDATE transactions SET return_date=?, status='RETURNED' WHERE id=? AND status='BORROWED'";
                try (PreparedStatement ps = conn.prepareStatement(close)) {
                    ps.setDate(1, Date.valueOf(LocalDate.now()));
                    ps.setInt(2, transactionId);
                    if (ps.executeUpdate() == 0) {
                        throw new LibraryException("That book has already been returned.");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE books SET available_copies = available_copies + 1 WHERE id=?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    public List<LoanRecord> getAllTransactions() throws SQLException {
        synchronized (DbLock.INSTANCE) {
            return runQuery(SELECT_LOANS + "ORDER BY t.borrow_date DESC, t.id DESC", null);
        }
    }

    public List<LoanRecord> getTransactionsForMember(int memberId) throws SQLException {
        synchronized (DbLock.INSTANCE) {
            return runQuery(SELECT_LOANS + "WHERE t.member_id=? ORDER BY t.borrow_date DESC, t.id DESC", memberId);
        }
    }

    /** Number of books past their due date and not yet returned. */
    public int countOverdue() throws SQLException {
        synchronized (DbLock.INSTANCE) {
            String sql = "SELECT COUNT(*) FROM transactions WHERE status='BORROWED' AND due_date < CURDATE()";
            try (Statement st = DBConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Books borrowed per month for the last `months` months (oldest first, empty months = 0). */
    public Map<String, Integer> getLoansPerMonth(int months) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            result.put(YearMonth.now().minusMonths(i).toString(), 0); // e.g. "2026-10"
        }
        LocalDate start = YearMonth.now().minusMonths(months - 1).atDay(1);
        String sql = "SELECT DATE_FORMAT(borrow_date, '%Y-%m') AS ym, COUNT(*) AS c FROM transactions " +
                "WHERE borrow_date >= ? GROUP BY ym ORDER BY ym";
        synchronized (DbLock.INSTANCE) {
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(start));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString("ym"), rs.getInt("c"));
                    }
                }
            }
        }
        return result;
    }

    /** The most borrowed book titles, most popular first. */
    public Map<String, Integer> getTopBorrowedBooks(int limit) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT b.title, COUNT(*) AS c FROM transactions t JOIN books b ON t.book_id=b.id " +
                "GROUP BY b.id, b.title ORDER BY c DESC, b.title LIMIT ?";
        synchronized (DbLock.INSTANCE) {
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.put(rs.getString("title"), rs.getInt("c"));
                    }
                }
            }
        }
        return result;
    }

    private int countActiveLoans(Connection conn, int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE member_id=? AND status='BORROWED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<LoanRecord> runQuery(String sql, Integer memberId) throws SQLException {
        List<LoanRecord> rows = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            if (memberId != null) ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date returned = rs.getDate("return_date");
                    rows.add(new LoanRecord(
                            rs.getInt("id"),
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("full_name"),
                            rs.getDate("borrow_date").toLocalDate(),
                            rs.getDate("due_date").toLocalDate(),
                            returned == null ? null : returned.toLocalDate(),
                            rs.getString("status")));
                }
            }
        }
        return rows;
    }
}
