package com.library.model;

import java.time.LocalDate;

/** One row of the transactions table: a book borrowed by a member. */
public class LoanRecord {
    private final int id;
    private final int bookId;
    private final String bookTitle;
    private final String memberName;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate; // null while the book is still out
    private final String status;        // BORROWED or RETURNED

    public LoanRecord(int id, int bookId, String bookTitle, String memberName,
                      LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, String status) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public int getId() { return id; }
    public int getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getMemberName() { return memberName; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getStatus() { return status; }

    public boolean isOverdue() {
        return returnDate == null && dueDate.isBefore(LocalDate.now());
    }

    /** What the tables show in the Status column. */
    public String getDisplayStatus() {
        return isOverdue() ? "OVERDUE" : status;
    }
}
