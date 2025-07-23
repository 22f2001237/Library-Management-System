package com.library.dao;

import com.library.model.Borrower;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BorrowerDAO {

    /**
     * Creates a new loan record in the database.
     * @param borrower The Borrower object to create.
     * @return The Borrower object with its auto-generated ID, or null if creation fails.
     */
    public Borrower createLoan(Borrower borrower) { // Parameter name changed for consistency
        String sql = "INSERT INTO loans (book_id, member_id, loan_date, due_date, renewed, fine_amount, fine_paid) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, borrower.getBookId());
            pstmt.setInt(2, borrower.getMemberId());
            pstmt.setDate(3, java.sql.Date.valueOf(borrower.getLoanDate()));
            pstmt.setDate(4, java.sql.Date.valueOf(borrower.getDueDate()));
            pstmt.setBoolean(5, borrower.isRenewed());
            pstmt.setDouble(6, borrower.getFineAmount()); // New: Set initial fine amount (0.00)
            pstmt.setBoolean(7, borrower.isFinePaid());   // New: Set initial fine paid status (FALSE)

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        borrower.setLoanId(generatedKeys.getInt(1));
                        System.out.println("Borrower entry created successfully with ID: " + borrower.getLoanId());
                        return borrower;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating borrower entry: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a loan (borrower entry) by its ID.
     * @param loanId The ID of the loan to retrieve.
     * @return The Borrower object, or null if not found.
     */
    public Borrower getLoanById(int loanId) {
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed, fine_amount, fine_paid FROM loans WHERE loan_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, loanId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LocalDate returnDate = rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null;
                    return new Borrower(
                        rs.getInt("loan_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        returnDate,
                        rs.getBoolean("renewed"),
                        rs.getDouble("fine_amount"), // New: Retrieve fine amount
                        rs.getBoolean("fine_paid")   // New: Retrieve fine paid status
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting borrower entry by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all loans (borrower entries) for a specific member.
     * @param memberId The ID of the member.
     * @return A list of Borrower objects for the member.
     */
    public List<Borrower> getLoansByMemberId(int memberId) {
        List<Borrower> borrowers = new ArrayList<>(); // Changed list name
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed, fine_amount, fine_paid FROM loans WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate returnDate = rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null;
                    borrowers.add(new Borrower(
                        rs.getInt("loan_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        returnDate,
                        rs.getBoolean("renewed"),
                        rs.getDouble("fine_amount"),
                        rs.getBoolean("fine_paid")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting borrower entries by member ID: " + e.getMessage());
            e.printStackTrace();
        }
        return borrowers;
    }

    /**
     * Retrieves active loans (not yet returned) for a specific member.
     * @param memberId The ID of the member.
     * @return A list of active Borrower objects for the member.
     */
    public List<Borrower> getActiveLoansByMemberId(int memberId) {
        List<Borrower> borrowers = new ArrayList<>(); // Changed list name
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed, fine_amount, fine_paid FROM loans WHERE member_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    borrowers.add(new Borrower(
                        rs.getInt("loan_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        null, // return_date is null for active loans
                        rs.getBoolean("renewed"),
                        rs.getDouble("fine_amount"),
                        rs.getBoolean("fine_paid")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting active borrower entries by member ID: " + e.getMessage());
            e.printStackTrace();
        }
        return borrowers;
    }

    /**
     * Updates the return date and fine amount/status of a loan (borrower entry).
     * @param loanId The ID of the loan to update.
     * @param returnDate The date the book was returned.
     * @param fineAmount The fine calculated for this loan.
     * @param finePaid The initial fine paid status (usually false when first set).
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateLoanReturnDate(int loanId, LocalDate returnDate, double fineAmount, boolean finePaid) { // Changed signature
        String sql = "UPDATE loans SET return_date = ?, fine_amount = ?, fine_paid = ? WHERE loan_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(returnDate));
            pstmt.setDouble(2, fineAmount); // New: Set fine amount
            pstmt.setBoolean(3, finePaid);  // New: Set fine paid status
            pstmt.setInt(4, loanId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating borrower entry return date: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the due date and sets the renewed status of a loan (borrower entry).
     * Fine amount and paid status are not changed here, only on return.
     * @param loanId The ID of the loan to update.
     * @param newDueDate The new due date after renewal.
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateLoanRenewedStatus(int loanId, LocalDate newDueDate) {
        String sql = "UPDATE loans SET due_date = ?, renewed = TRUE WHERE loan_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(newDueDate));
            pstmt.setInt(2, loanId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating borrower entry renewed status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the fine_paid status for a specific borrower entry.
     * @param loanId The ID of the borrower entry.
     * @param paidStatus The new payment status (true for paid, false for unpaid).
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateFinePaidStatus(int loanId, boolean paidStatus) { // New method
        String sql = "UPDATE loans SET fine_paid = ? WHERE loan_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, paidStatus);
            pstmt.setInt(2, loanId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating fine paid status for borrower entry: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all active loans (borrower entries) that are overdue.
     * @param currentDate The current date to check overdue against.
     * @return A list of overdue Borrower objects.
     */
    public List<Borrower> getOverdueLoans(LocalDate currentDate) {
        List<Borrower> overdueLoans = new ArrayList<>();
        // Note: fine_amount and fine_paid are included in the SELECT statement
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed, fine_amount, fine_paid FROM loans WHERE return_date IS NULL AND due_date < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(currentDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    overdueLoans.add(new Borrower(
                        rs.getInt("loan_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        null, // return_date is null for active overdue loans
                        rs.getBoolean("renewed"),
                        rs.getDouble("fine_amount"),
                        rs.getBoolean("fine_paid")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting overdue borrower entries: " + e.getMessage());
            e.printStackTrace();
        }
        return overdueLoans;
    }
}
