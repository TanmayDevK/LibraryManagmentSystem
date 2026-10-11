package com.library.model;

public class Librarian extends User {

    public Librarian(int id, String username, String fullName, String email) {
        super(id, username, fullName, email);
    }

    @Override
    public String getRole() { return "LIBRARIAN"; }

    @Override
    public int getBorrowLimit() { return 0; } // librarians manage books, they don't borrow

    @Override
    public String getDashboardTitle() { return "Librarian Dashboard - " + getFullName(); }
}
