package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BorrowerDAO;
import com.library.dao.MemberDAO;
import com.library.model.Book;
import com.library.model.Borrower;
import com.library.model.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LibraryService {

    private static final int MAX_BORROWED_BOOKS = 4;
    private static final int INITIAL_LOAN_DAYS = 5;
    private static final int RENEWAL_DAYS = 3;

    private BookDAO bookDAO;
    private MemberDAO memberDAO;
    private BorrowerDAO borrowerDAO;

    public LibraryService(BookDAO bookDAO, MemberDAO memberDAO, BorrowerDAO borrowerDAO) {
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
        this.borrowerDAO = borrowerDAO;
    }

    // --- User Actions ---

    /**
     * Allows a member to borrow a book.
     * @param memberId The ID of the member.
     * @param bookId The ID of the book.
     * @return true if the book was successfully borrowed, false otherwise.
     */
    public boolean borrowBook(int memberId, int bookId) {
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            System.out.println("Error: Member with ID " + memberId + " not found.");
            return false;
        }

        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            System.out.println("Error: Book with ID " + bookId + " not found.");
            return false;
        }

        if (book.getAvailableCopies() <= 0) {
            System.out.println("Error: Book '" + book.getTitle() + "' is currently not available.");
            return false;
        }

        List<Borrower> activeLoans = borrowerDAO.getActiveLoansByMemberId(memberId);
        if (activeLoans.size() >= MAX_BORROWED_BOOKS) {
            System.out.println("Error: Member " + member.getFirstName() + " has reached the maximum limit of " + MAX_BORROWED_BOOKS + " borrowed books.");
            return false;
        }

        Optional<Borrower> existingActiveLoan = activeLoans.stream()
            .filter(loan -> loan.getBookId() == bookId)
            .findFirst();

        if (existingActiveLoan.isPresent()) {
            System.out.println("Error: Member " + member.getFirstName() + " already has an active loan for '" + book.getTitle() + "'.");
            System.out.println("Please return the current copy before borrowing it again.");
            return false;
        }

        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(INITIAL_LOAN_DAYS);
        Borrower newBorrowerEntry = new Borrower(bookId, memberId, loanDate, dueDate);

        if (borrowerDAO.createLoan(newBorrowerEntry) != null) {
            bookDAO.updateBookCopies(bookId, -1);
            System.out.println("Book '" + book.getTitle() + "' borrowed successfully by " + member.getFirstName() + ".");
            System.out.println("Due date: " + dueDate);
            return true;
        }
        System.out.println("Failed to create borrower entry.");
        return false;
    }

    /**
     * Allows a member to return a book.
     * This method now calculates and updates the fine in the database.
     * @param loanId The ID of the loan to return.
     * @return The fine amount if any, otherwise 0.0.
     */
    public double returnBook(int loanId) {
        Borrower borrowerEntry = borrowerDAO.getLoanById(loanId);
        if (borrowerEntry == null) {
            System.out.println("Error: Borrower entry with ID " + loanId + " not found.");
            return 0.0;
        }
        if (borrowerEntry.getReturnDate() != null) {
            System.out.println("Book for borrower entry ID " + loanId + " has already been returned.");
            System.out.println("Fine previously incurred: Rs. " + String.format("%.2f", borrowerEntry.getFineAmount()));
            return borrowerEntry.getFineAmount();
        }

        LocalDate returnDate = LocalDate.now();
        double calculatedFine = borrowerEntry.calculateFine(returnDate);

        if (borrowerDAO.updateLoanReturnDate(loanId, returnDate, calculatedFine, false)) {
            bookDAO.updateBookCopies(borrowerEntry.getBookId(), 1);

            if (calculatedFine > 0) {
                Member member = memberDAO.getMemberById(borrowerEntry.getMemberId());
                if (member != null) {
                    double newTotalFineDue = member.getTotalFineDue() + calculatedFine;
                    memberDAO.updateTotalFineDue(member.getMemberId(), newTotalFineDue);
                    System.out.println("Member's total fine due updated to Rs. " + String.format("%.2f", newTotalFineDue));
                }
            }

            System.out.println("Book for borrower entry ID " + loanId + " returned successfully.");
            if (calculatedFine > 0) {
                System.out.println("Fine incurred: Rs. " + String.format("%.2f", calculatedFine));
            } else {
                System.out.println("No fine incurred.");
            }
            return calculatedFine;
        }
        System.out.println("Failed to update borrower entry return date.");
        return 0.0;
    }

    /**
     * Allows a member to renew a book.
     * @param loanId The ID of the loan to renew.
     * @return true if renewed successfully, false otherwise.
     */
    public boolean renewBook(int loanId) {
        Borrower borrowerEntry = borrowerDAO.getLoanById(loanId);
        if (borrowerEntry == null) {
            System.out.println("Error: Borrower entry with ID " + loanId + " not found.");
            return false;
        }
        if (borrowerEntry.getReturnDate() != null) {
            System.out.println("Error: Cannot renew a book that has already been returned.");
            return false;
        }
        if (borrowerEntry.isRenewed()) {
            System.out.println("Error: This book has already been renewed once.");
            return false;
        }

        LocalDate newDueDate = borrowerEntry.getDueDate().plusDays(RENEWAL_DAYS);
        if (borrowerDAO.updateLoanRenewedStatus(loanId, newDueDate)) {
            System.out.println("Book for borrower entry ID " + loanId + " renewed successfully. New due date: " + newDueDate);
            return true;
        }
        System.out.println("Failed to renew book for borrower entry ID " + loanId + ".");
        return false;
    }

    /**
     * Allows a member to pay their outstanding fines.
     * @param memberId The ID of the member paying the fine.
     * @return true if fines were successfully processed/paid, false otherwise.
     */
    public boolean payFines(int memberId) {
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            System.out.println("Error: Member with ID " + memberId + " not found.");
            return false;
        }

        if (member.getTotalFineDue() <= 0) {
            System.out.println("Member " + member.getFirstName() + " has no outstanding fines.");
            return true;
        }

        List<Borrower> memberLoansWithUnpaidFines = borrowerDAO.getLoansByMemberId(memberId).stream()
            .filter(loan -> loan.getFineAmount() > 0 && !loan.isFinePaid())
            .collect(Collectors.toList());

        if (memberLoansWithUnpaidFines.isEmpty()) {
            System.out.println("Member " + member.getFirstName() + " has no individual loan fines outstanding, but total fine due is " + String.format("%.2f", member.getTotalFineDue()) + ". This might indicate a data inconsistency.");
            if (member.getTotalFineDue() > 0) {
                if (memberDAO.updateTotalFineDue(memberId, 0.00)) {
                    System.out.println("Total fine due for member " + member.getFirstName() + " reset to 0.00.");
                    return true;
                } else {
                    System.out.println("Failed to reset total fine due for member " + member.getFirstName() + ".");
                    return false;
                }
            }
            return true;
        }

        boolean allFinesPaidSuccessfully = true;
        for (Borrower loan : memberLoansWithUnpaidFines) {
            if (!borrowerDAO.updateFinePaidStatus(loan.getLoanId(), true)) {
                allFinesPaidSuccessfully = false;
                System.err.println("Failed to mark fine as paid for Borrower Entry ID: " + loan.getLoanId());
            }
        }

        if (allFinesPaidSuccessfully) {
            if (memberDAO.updateTotalFineDue(memberId, 0.00)) {
                System.out.println("All outstanding fines for " + member.getFirstName() + " (Rs. " + String.format("%.2f", member.getTotalFineDue()) + ") have been paid.");
                return true;
            } else {
                System.out.println("Successfully marked individual loan fines as paid, but failed to reset member's total fine due.");
                return false;
            }
        } else {
            System.out.println("Some fines could not be marked as paid. Please check logs.");
            return false;
        }
    }


    /**
     * Checks the availability of books based on a search query.
     * @param query The search string (title, author, or ISBN).
     * @return A list of available books matching the query.
     */
    public List<Book> checkBookAvailability(String query) {
        List<Book> foundBooks = bookDAO.searchBooks(query);
        return foundBooks.stream()
                         .filter(book -> book.getAvailableCopies() > 0)
                         .collect(Collectors.toList());
    }

    /**
     * Gets all books currently borrowed by a specific member.
     * @param memberId The ID of the member.
     * @return A list of Borrower objects representing borrowed books.
     */
    public List<Borrower> getBorrowedBooks(int memberId) {
        return borrowerDAO.getActiveLoansByMemberId(memberId);
    }

    /**
     * Calculates the total fine for a member based on their overdue loans.
     * This method now retrieves the total_fine_due from the member table.
     * It also lists individual unpaid fines.
     * @param memberId The ID of the member.
     * @return The total fine amount.
     */
    public double getMemberFineDetails(int memberId) {
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            System.out.println("Error: Member with ID " + memberId + " not found.");
            return 0.0;
        }

        System.out.println("\n--- Fine Details for Member: " + member.getFirstName() + " " + member.getLastName() + " (ID: " + member.getMemberId() + ") ---");
        System.out.println("Total Outstanding Fine: Rs. " + String.format("%.2f", member.getTotalFineDue()));

        List<Borrower> allLoans = borrowerDAO.getLoansByMemberId(memberId);
        List<Borrower> unpaidFines = allLoans.stream()
            .filter(loan -> loan.getFineAmount() > 0 && !loan.isFinePaid())
            .collect(Collectors.toList());

        if (unpaidFines.isEmpty()) {
            System.out.println("No individual loan fines currently outstanding.");
        } else {
            System.out.println("Details of Unpaid Fines:");
            for (Borrower loan : unpaidFines) {
                Book book = bookDAO.getBookById(loan.getBookId());
                System.out.println("  - Borrower Entry ID: " + loan.getLoanId() +
                                   ", Book: " + (book != null ? book.getTitle() : "Unknown") +
                                   ", Due Date: " + loan.getDueDate() +
                                   ", Fine Amount: Rs. " + String.format("%.2f", loan.getFineAmount()));
            }
        }
        return member.getTotalFineDue();
    }

    // --- Librarian Actions ---

    /**
     * Adds a new book to the library catalog (Librarian feature).
     * @param book The Book object to add.
     * @return true if added successfully, false otherwise.
     */
    public boolean addBook(Book book) {
        return bookDAO.addBook(book) != null;
    }

    /**
     * Checks the status of a user (Librarian feature).
     * @param memberId The ID of the member.
     */
    public void getMemberStatus(int memberId) {
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            System.out.println("Member with ID " + memberId + " not found.");
            return;
        }
        System.out.println("\n--- Member Status for: " + member.getFirstName() + " " + member.getLastName() + " (ID: " + member.getMemberId() + ") ---");
        System.out.println("Email: " + member.getEmail());
        System.out.println("Phone: " + (member.getPhoneNumber() != null ? member.getPhoneNumber() : "N/A"));
        System.out.println("Joined: " + member.getJoinDate());
        System.out.println("Total Outstanding Fine: Rs. " + String.format("%.2f", member.getTotalFineDue()));

        List<Borrower> activeLoans = borrowerDAO.getActiveLoansByMemberId(memberId);
        System.out.println("\nBooks Borrowed (" + activeLoans.size() + "/" + MAX_BORROWED_BOOKS + "):");
        if (activeLoans.isEmpty()) {
            System.out.println("  No books currently borrowed.");
        } else {
            activeLoans.forEach(borrowerEntry -> {
                Book book = bookDAO.getBookById(borrowerEntry.getBookId());
                System.out.println("  - Borrower Entry ID: " + borrowerEntry.getLoanId() +
                                   ", Book: " + (book != null ? book.getTitle() : "Unknown") +
                                   ", Due Date: " + borrowerEntry.getDueDate() +
                                   ", Renewed: " + (borrowerEntry.isRenewed() ? "Yes" : "No"));
            });
        }
    }

    /**
     * Retrieves all available books in the library (Librarian feature).
     * @return A list of available Book objects.
     */
    public List<Book> getAllAvailableBooks() {
        return bookDAO.getAvailableBooks();
    }

    /**
     * Retrieves and displays a list of all members with their total outstanding fines.
     * (Librarian feature)
     */
    public void viewAllMembersWithFines() { // NEW METHOD
        List<Member> allMembers = memberDAO.getAllMembers();
        if (allMembers.isEmpty()) {
            System.out.println("No members registered in the system.");
            return;
        }

        System.out.println("\n--- All Members and Outstanding Fines ---");
        System.out.printf("%-10s %-25s %-15s%n", "Member ID", "Member Name", "Total Fine Due");
        System.out.println("--------------------------------------------------");
        for (Member member : allMembers) {
            System.out.printf("%-10d %-25s Rs. %-15.2f%n",
                              member.getMemberId(),
                              member.getFirstName() + " " + member.getLastName(),
                              member.getTotalFineDue());
        }
        System.out.println("--------------------------------------------------");
    }


    /**
     * Deletes a book from the library (Librarian feature).
     * @param bookId The ID of the book to delete.
     * @return true if deleted successfully, false otherwise.
     */
    public boolean deleteBook(int bookId) {
        return bookDAO.deleteBook(bookId);
    }

    /**
     * Gets a report of all overdue loans (Librarian feature).
     * @return A list of overdue Borrower objects.
     */
    public List<Borrower> getOverdueLoansReport() {
        LocalDate currentDate = LocalDate.now();
        return borrowerDAO.getOverdueLoans(currentDate);
    }
}
