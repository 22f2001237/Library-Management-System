package com.library.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BorrowerDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/library_system";
    private static final String USER = "root";
    private static final String PASSWORD = "kavih2530";

    public void createBorrowing(Borrower b) {
        String insertSql = "INSERT INTO borrowings (user_id, book_id, borrow_date, due_date, renewal_count) VALUES (?, ?, ?, ?, ?)";
        String updateBookSql = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateBookSql)) {

                updateStmt.setInt(1, b.getBookId());
                int rowsUpdated = updateStmt.executeUpdate();

                if (rowsUpdated == 0) {
                    System.out.println("❌ Book is not available.");
                    conn.rollback();
                    return;
                }

                insertStmt.setInt(1, b.getUserId());
                insertStmt.setInt(2, b.getBookId());
                insertStmt.setDate(3, b.getBorrowDate());
                insertStmt.setDate(4, b.getDueDate());
                insertStmt.setBoolean(5, b.isRenewed());
                insertStmt.executeUpdate();

                conn.commit();
                System.out.println("✅ Borrowing created and book availability updated.");
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateBorrowingReturnDate(int borrowingId, LocalDate returnDate) {
        String updateReturnSql = "UPDATE borrowings SET return_date = ? WHERE borrowing_id = ?";
        String updateBookSql = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = (SELECT book_id FROM borrowings WHERE borrowing_id = ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateReturnStmt = conn.prepareStatement(updateReturnSql);
                 PreparedStatement updateBookStmt = conn.prepareStatement(updateBookSql)) {

                updateReturnStmt.setDate(1, Date.valueOf(returnDate));
                updateReturnStmt.setInt(2, borrowingId);
                int rows = updateReturnStmt.executeUpdate();

                if (rows > 0) {
                    updateBookStmt.setInt(1, borrowingId);
                    updateBookStmt.executeUpdate();
                    conn.commit();
                    System.out.println("✅ Returned successfully and availability updated.");
                } else {
                    conn.rollback();
                    System.out.println("❌ Borrowing not found.");
                }
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateBorrowingRenewedStatus(int borrowingId) {
        String selectSql = "SELECT due_date FROM borrowings WHERE borrowing_id = ?";
        String updateSql = "UPDATE borrowings SET due_date = ?, renewal_count = true WHERE borrowing_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {

            selectStmt.setInt(1, borrowingId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                Date dueDateSql = rs.getDate("due_date");
                if (dueDateSql == null) {
                    System.out.println("❌ Current due date is null. Cannot renew.");
                    return;
                }
                LocalDate currentDueDate = dueDateSql.toLocalDate();
                LocalDate newDueDate = currentDueDate.plusDays(3);

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setDate(1, Date.valueOf(newDueDate));
                    updateStmt.setInt(2, borrowingId);
                    int rows = updateStmt.executeUpdate();
                    System.out.println(rows > 0 ? "✅ Renewed for 3 more days." : "❌ Update failed.");
                }
            } else {
                System.out.println("❌ Borrowing not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Borrower> getOverdueBorrowings() {
        List<Borrower> list = new ArrayList<>();
        String sql = "SELECT * FROM borrowings WHERE return_date IS NULL AND due_date < CURDATE()";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Borrower b = new Borrower(
                    rs.getInt("borrowing_id"),
                    rs.getInt("user_id"),
                    rs.getInt("book_id"),
                    rs.getDate("borrow_date"),
                    rs.getDate("due_date"),
                    rs.getDate("return_date"),
                    rs.getBoolean("renewal_count")
                );
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
