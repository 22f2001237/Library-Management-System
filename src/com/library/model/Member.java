package com.library.model;

import java.time.LocalDate;

public class Member {
    private int memberId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate joinDate;
    private double totalFineDue; // New: Total unpaid fine for this member

    public Member(int memberId, String firstName, String lastName, String email, String phoneNumber, LocalDate joinDate, double totalFineDue) {
        this.memberId = memberId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.joinDate = joinDate;
        this.totalFineDue = totalFineDue; // Initialize new field
    }

    // Constructor without memberId for new members
    public Member(String firstName, String lastName, String email, String phoneNumber, LocalDate joinDate) {
        this(0, firstName, lastName, email, phoneNumber, joinDate, 0.00); // Initialize new field
    }

    // Getters
    public int getMemberId() { return memberId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDate getJoinDate() { return joinDate; }
    public double getTotalFineDue() { return totalFineDue; } // New Getter

    // Setters
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public void setTotalFineDue(double totalFineDue) { this.totalFineDue = totalFineDue; } // New Setter

    @Override
    public String toString() {
        return "Member [ID=" + memberId + ", Name=" + firstName + " " + lastName +
               ", Email=" + email + ", Phone=" + phoneNumber + ", Joined=" + joinDate +
               ", Total Fine Due=Rs. " + String.format("%.2f", totalFineDue) + "]";
    }
}
