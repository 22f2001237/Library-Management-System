package com.library.model;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private String isbn;
    private int publicationYear;
    private int totalCopies;
    private int availableCopies;

    public Book(int bookId, String title, String author, String isbn, int publicationYear, int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    // Constructor without bookId for new books (ID will be auto-generated by DB)
    public Book(String title, String author, String isbn, int publicationYear, int totalCopies, int availableCopies) {
        this(0, title, author, isbn, publicationYear, totalCopies, availableCopies);
    }

    // Getters
    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getPublicationYear() { return publicationYear; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    // Setters (for updates or setting auto-generated ID)
    public void setBookId(int bookId) { this.bookId = bookId; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    @Override
    public String toString() {
        return "Book [ID=" + bookId + ", Title='" + title + "', Author='" + author +
               "', ISBN='" + isbn + "', Year=" + publicationYear +
               ", Total Copies=" + totalCopies + ", Available Copies=" + availableCopies + "]";
    }
}
