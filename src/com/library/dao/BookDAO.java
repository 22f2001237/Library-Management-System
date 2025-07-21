package com.library.dao;

import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    private Connection conn;

    public BookDAO(Connection conn) {
        this.conn = conn;
    }

    // Add a new book
    public void addBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (title, author, isbn, total_copies, available_copies) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getIsbn());
            stmt.setInt(4, book.getTotalCopies());
            stmt.setInt(5, book.getAvailableCopies());
            stmt.executeUpdate();
        }
    }

    // Get book by ID
    public Book getBookById(int bookId) throws SQLException {
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Book(
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getInt("total_copies"),
                    rs.getInt("available_copies")
                );
            }
        }
        return null;
    }

    // Get all available books (where available copies > 0)
    public List<Book> getAvailableBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE available_copies > 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getInt("total_copies"),
                    rs.getInt("available_copies")
                ));
            }
        }
        return books;
    }

    // ✅ Get all books (regardless of availability)
    public List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getInt("total_copies"),
                    rs.getInt("available_copies")
                ));
            }
        }
        return books;
    }

    // Update available copies after borrowing or returning
    public void updateAvailableCopies(int bookId, int newAvailableCount) throws SQLException {
        String sql = "UPDATE books SET available_copies = ? WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newAvailableCount);
            stmt.setInt(2, bookId);
            stmt.executeUpdate();
        }
    }

    // Increment total and available copies if librarian adds more of the same book
    public void incrementCopies(int bookId, int additionalCopies) throws SQLException {
        String sql = "UPDATE books SET total_copies = total_copies + ?, available_copies = available_copies + ? WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, additionalCopies);
            stmt.setInt(2, additionalCopies);
            stmt.setInt(3, bookId);
            stmt.executeUpdate();
        }
    }
}

