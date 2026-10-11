package com.library.model;

public class Member extends User {

    public static final int MAX_BOOKS = 3;

    public Member(int id, String username, String fullName, String email) {
        super(id, username, fullName, email);
    }

    @Override
    public String getRole() { return "MEMBER"; }

    @Override
    public int getBorrowLimit() { return MAX_BOOKS; }

    @Override
    public String getDashboardTitle() { return "Member Dashboard - " + getFullName(); }
}
