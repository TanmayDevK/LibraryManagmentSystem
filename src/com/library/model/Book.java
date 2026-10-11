package com.library.model;

public class Book {
    private final int id;
    private final String title;
    private final String author;
    private final String isbn;
    private final String genre;
    private final int totalCopies;
    private final int availableCopies;

    public Book(int id, String title, String author, String isbn, String genre,
                int totalCopies, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getGenre() { return genre; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    @Override
    public String toString() {
        return title + " by " + author;
    }
}
