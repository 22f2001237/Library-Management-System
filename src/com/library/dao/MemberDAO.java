
package com.library.dao;

import com.library.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    /**
     * Adds a new member to the database.
     * @param member The Member object to add.
     * @return The Member object with its auto-generated ID, or null if insertion fails.
     */
    public Member addMember(Member member) {
        // total_fine_due is added to the INSERT statement
        String sql = "INSERT INTO members (first_name, last_name, email, phone_number, join_date, total_fine_due) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, member.getFirstName());
            pstmt.setString(2, member.getLastName());
            pstmt.setString(3, member.getEmail());
            pstmt.setString(4, member.getPhoneNumber());
            pstmt.setDate(5, java.sql.Date.valueOf(member.getJoinDate()));
            pstmt.setDouble(6, member.getTotalFineDue()); // New: Set initial total fine due (0.00)

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        member.setMemberId(generatedKeys.getInt(1));
                        System.out.println("Member added successfully with ID: " + member.getMemberId());
                        return member;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding member: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a member by their ID.
     * @param memberId The ID of the member to retrieve.
     * @return The Member object, or null if not found.
     */
    public Member getMemberById(int memberId) {
        // total_fine_due is added to the SELECT statement
        String sql = "SELECT member_id, first_name, last_name, email, phone_number, join_date, total_fine_due FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Member(
                        rs.getInt("member_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getDate("join_date").toLocalDate(),
                        rs.getDouble("total_fine_due") // New: Retrieve total fine due
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting member by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a member by their email address.
     * @param email The email address of the member to retrieve.
     * @return The Member object, or null if not found.
     */
    public Member getMemberByEmail(String email) {
        // total_fine_due is added to the SELECT statement
        String sql = "SELECT member_id, first_name, last_name, email, phone_number, join_date, total_fine_due FROM members WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Member(
                        rs.getInt("member_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone_number"),
                        rs.getDate("join_date").toLocalDate(),
                        rs.getDouble("total_fine_due") // New: Retrieve total fine due
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting member by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Updates an existing member's details in the database.
     * total_fine_due is NOT updated here, it's handled by a separate method.
     * @param member The Member object with updated details.
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateMember(Member member) {
        String sql = "UPDATE members SET first_name = ?, last_name = ?, email = ?, phone_number = ? WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, member.getFirstName());
            pstmt.setString(2, member.getLastName());
            pstmt.setString(3, member.getEmail());
            pstmt.setString(4, member.getPhoneNumber());
            pstmt.setInt(5, member.getMemberId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating member: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves all members from the database.
     * @return A list of all Member objects.
     */
    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        // total_fine_due is added to the SELECT statement
        String sql = "SELECT member_id, first_name, last_name, email, phone_number, join_date, total_fine_due FROM members";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                members.add(new Member(
                    rs.getInt("member_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone_number"),
                    rs.getDate("join_date").toLocalDate(),
                    rs.getDouble("total_fine_due") // New: Retrieve total fine due
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all members: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Updates the total_fine_due for a member.
     * @param memberId The ID of the member.
     * @param newTotalFineDue The new total fine amount for the member.
     * @return true if updated successfully, false otherwise.
     */
    public boolean updateTotalFineDue(int memberId, double newTotalFineDue) { // New method
        String sql = "UPDATE members SET total_fine_due = ? WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newTotalFineDue);
            pstmt.setInt(2, memberId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating total fine due for member: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a member from the database by ID.
     * @param memberId The ID of the member to delete.
     * @return true if deleted successfully, false otherwise.
     */
    public boolean deleteMember(int memberId) {
        String sql = "DELETE FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, memberId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting member: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
