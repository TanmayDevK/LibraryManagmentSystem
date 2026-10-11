package com.library.model;

/**
 * Abstract base class for everyone who can log in.
 * Librarian and Member extend it and give their own behaviour
 * (runtime polymorphism through the abstract methods below).
 */
public abstract class User {
    private final int id;
    private final String username;
    private final String fullName;
    private final String email;

    protected User(int id, String username, String fullName, String email) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    /** "LIBRARIAN" or "MEMBER". */
    public abstract String getRole();

    /** Maximum number of books this kind of user may hold at once. */
    public abstract int getBorrowLimit();

    /** Window title for this user's dashboard. */
    public abstract String getDashboardTitle();

    public boolean isLibrarian() {
        return "LIBRARIAN".equals(getRole());
    }

    @Override
    public String toString() {
        return getRole() + ": " + fullName + " (" + username + ")";
    }
}
