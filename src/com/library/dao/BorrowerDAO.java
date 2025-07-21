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
     * @param loan The Loan object to create.
     * @return The Loan object with its auto-generated ID, or null if creation fails.
     */
    public Borrower createLoan(Borrower loan) {
        String sql = "INSERT INTO loans (book_id, member_id, loan_date, due_date, renewed) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, loan.getBookId());
            pstmt.setInt(2, loan.getMemberId());
            pstmt.setDate(3, java.sql.Date.valueOf(loan.getLoanDate()));
            pstmt.setDate(4, java.sql.Date.valueOf(loan.getDueDate()));
            pstmt.setBoolean(5, loan.isRenewed());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        loan.setLoanId(generatedKeys.getInt(1));
                        System.out.println("Loan created successfully with ID: " + loan.getLoanId());
                        return loan;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating loan: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a loan by its ID.
     * @param loanId The ID of the loan to retrieve.
     * @return The Loan object, or null if not found.
     */
    public Borrower getLoanById(int loanId) {
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed FROM loans WHERE loan_id = ?";
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
                        rs.getBoolean("renewed")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting loan by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all loans for a specific member.
     * @param memberId The ID of the member.
     * @return A list of Loan objects for the member.
     */
    public List<Borrower> getLoansByMemberId(int memberId) {
        List<Borrower> loans = new ArrayList<>();
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed FROM loans WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate returnDate = rs.getDate("return_date") != null ? rs.getDate("return_date").toLocalDate() : null;
                    loans.add(new Borrower(
                        rs.getInt("loan_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        returnDate,
                        rs.getBoolean("renewed")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting loans by member ID: " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    /**
     * Retrieves active loans (not yet returned) for a specific member.
     * @param memberId The ID of the member.
     * @return A list of active Loan objects for the member.
     */
    public List<Borrower> getActiveLoansByMemberId(int memberId) {
        List<Borrower> loans = new ArrayList<>();
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed FROM loans WHERE member_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    loans.add(new Borrower(
                        rs.getInt("loan_id"),
                        rs.getInt("book_id"),
                        rs.getInt("member_id"),
                        rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        null, // return_date is null for active loans
                        rs.getBoolean("renewed")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting active loans by member ID: " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    /**
     * Updates the return date of a loan.
     * @param loanId The ID of the loan to update.
     * @param returnDate The date the book was returned.
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateLoanReturnDate(int loanId, LocalDate returnDate) {
        String sql = "UPDATE loans SET return_date = ? WHERE loan_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(returnDate));
            pstmt.setInt(2, loanId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating loan return date: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the due date and sets the renewed status of a loan.
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
            System.err.println("Error updating loan renewed status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all active loans that are overdue.
     * @param currentDate The current date to check overdue against.
     * @return A list of overdue Loan objects.
     */
    public List<Borrower> getOverdueLoans(LocalDate currentDate) {
        List<Borrower> overdueLoans = new ArrayList<>();
        String sql = "SELECT loan_id, book_id, member_id, loan_date, due_date, return_date, renewed FROM loans WHERE return_date IS NULL AND due_date < ?";
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
                        rs.getBoolean("renewed")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting overdue loans: " + e.getMessage());
            e.printStackTrace();
        }
        return overdueLoans;
    }
}
