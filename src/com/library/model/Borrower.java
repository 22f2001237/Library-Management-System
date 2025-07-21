package com.library.model;

import java.sql.Date;

public class Borrower {
    private int borrowingId;
    private int userId;
    private int bookId;
    private Date borrowDate;
    private Date dueDate;
    private Date returnDate;
    private boolean renewed;

    public Borrower(int borrowingId, int userId, int bookId, Date borrowDate, Date dueDate, Date returnDate, boolean renewed) {
        this.borrowingId = borrowingId;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.renewed = renewed;
    }

    // Getters and setters
    public int getBorrowingId() { return borrowingId; }
    public void setBorrowingId(int borrowingId) { this.borrowingId = borrowingId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    public boolean isRenewed() { return renewed; }
    public void setRenewed(boolean renewed) { this.renewed = renewed; }

    @Override
    public String toString() {
        return String.format(
            "Borrowing ID: %d | User ID: %d | Book ID: %d | Borrowed: %s | Due: %s | Returned: %s | Renewed: %b",
            borrowingId, userId, bookId,
            borrowDate != null ? borrowDate.toString() : "N/A",
            dueDate != null ? dueDate.toString() : "N/A",
            returnDate != null ? returnDate.toString() : "Not Returned",
            renewed);
    }
}

