package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class LibraryService {

    private static final int MAX_BORROWED_BOOKS = 4;
    private static final int INITIAL_LOAN_DAYS = 5;
    private static final int RENEWAL_DAYS = 3;

    private BookDAO bookDAO;
    private MemberDAO memberDAO;
    private LoanDAO loanDAO;

    // Constructor for dependency injection
    public LibraryService(BookDAO bookDAO, MemberDAO memberDAO, LoanDAO loanDAO) {
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
        this.loanDAO = loanDAO;
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

        List<Loan> activeLoans = loanDAO.getActiveLoansByMemberId(memberId);
        if (activeLoans.size() >= MAX_BORROWED_BOOKS) {
            System.out.println("Error: Member " + member.getFirstName() + " has reached the maximum limit of " + MAX_BORROWED_BOOKS + " borrowed books.");
            return false;
        }

        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(INITIAL_LOAN_DAYS);
        Loan newLoan = new Loan(bookId, memberId, loanDate, dueDate);

        if (loanDAO.createLoan(newLoan) != null) {
            bookDAO.updateBookCopies(bookId, -1); // Decrement available copies
            System.out.println("Book '" + book.getTitle() + "' borrowed successfully by " + member.getFirstName() + ".");
            System.out.println("Due date: " + dueDate);
            return true;
        }
        System.out.println("Failed to create loan record.");
        return false;
    }

    /**
     * Allows a member to return a book.
     * @param loanId The ID of the loan to return.
     * @return The fine amount if any, otherwise 0.0.
     */
    public double returnBook(int loanId) {
        Loan loan = loanDAO.getLoanById(loanId);
        if (loan == null) {
            System.out.println("Error: Loan with ID " + loanId + " not found.");
            return 0.0;
        }
        if (loan.getReturnDate() != null) {
            System.out.println("Book for loan ID " + loanId + " has already been returned.");
            return loan.calculateFine(loan.getReturnDate()); // Return previously calculated fine
        }

        LocalDate returnDate = LocalDate.now();
        double fine = loan.calculateFine(returnDate);

        if (loanDAO.updateLoanReturnDate(loanId, returnDate)) {
            bookDAO.updateBookCopies(loan.getBookId(), 1); // Increment available copies
            System.out.println("Book for loan ID " + loanId + " returned successfully.");
            if (fine > 0) {
                System.out.println("Fine for overdue: Rs. " + fine);
            } else {
                System.out.println("No fine incurred.");
            }
            return fine;
        }
        System.out.println("Failed to update loan return date.");
        return 0.0;
    }

    /**
     * Allows a member to renew a book.
     * @param loanId The ID of the loan to renew.
     * @return true if renewed successfully, false otherwise.
     */
    public boolean renewBook(int loanId) {
        Loan loan = loanDAO.getLoanById(loanId);
        if (loan == null) {
            System.out.println("Error: Loan with ID " + loanId + " not found.");
            return false;
        }
        if (loan.getReturnDate() != null) {
            System.out.println("Error: Cannot renew a book that has already been returned.");
            return false;
        }
        if (loan.isRenewed()) {
            System.out.println("Error: This book has already been renewed once.");
            return false;
        }
        // Optional: Can add a check if the book is overdue before renewal
        // if (LocalDate.now().isAfter(loan.getDueDate())) {
        //     System.out.println("Error: Cannot renew an overdue book. Please return it first.");
        //     return false;
        // }

        LocalDate newDueDate = loan.getDueDate().plusDays(RENEWAL_DAYS);
        if (loanDAO.updateLoanRenewedStatus(loanId, newDueDate)) {
            System.out.println("Book for loan ID " + loanId + " renewed successfully. New due date: " + newDueDate);
            return true;
        }
        System.out.println("Failed to renew book for loan ID " + loanId + ".");
        return false;
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
     * @return A list of Loan objects representing borrowed books.
     */
    public List<Loan> getBorrowedBooks(int memberId) {
        return loanDAO.getActiveLoansByMemberId(memberId);
    }

    /**
     * Calculates the total fine for a member based on their overdue loans.
     * @param memberId The ID of the member.
     * @return The total fine amount.
     */
    public double getMemberFineDetails(int memberId) {
        List<Loan> activeLoans = loanDAO.getActiveLoansByMemberId(memberId);
        double totalFine = 0.0;
        LocalDate currentDate = LocalDate.now();

        System.out.println("\n--- Fine Details for Member ID: " + memberId + " ---");
        for (Loan loan : activeLoans) {
            double fine = loan.calculateFine(currentDate);
            if (fine > 0) {
                Book book = bookDAO.getBookById(loan.getBookId());
                System.out.println("  Loan ID: " + loan.getLoanId() +
                                   ", Book: " + (book != null ? book.getTitle() : "Unknown") +
                                   ", Due Date: " + loan.getDueDate() +
                                   ", Overdue Fine: Rs. " + fine);
                totalFine += fine;
            }
        }
        System.out.println("Total Fine: Rs. " + totalFine);
        return totalFine;
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

        List<Loan> activeLoans = loanDAO.getActiveLoansByMemberId(memberId);
        System.out.println("\nBooks Borrowed (" + activeLoans.size() + "/" + MAX_BORROWED_BOOKS + "):");
        if (activeLoans.isEmpty()) {
            System.out.println("  No books currently borrowed.");
        } else {
            activeLoans.forEach(loan -> {
                Book book = bookDAO.getBookById(loan.getBookId());
                System.out.println("  - Loan ID: " + loan.getLoanId() +
                                   ", Book: " + (book != null ? book.getTitle() : "Unknown") +
                                   ", Due Date: " + loan.getDueDate() +
                                   ", Renewed: " + (loan.isRenewed() ? "Yes" : "No"));
            });
        }
        getMemberFineDetails(memberId); // This will print fine details
    }

    /**
     * Retrieves all available books in the library (Librarian feature).
     * @return A list of available Book objects.
     */
    public List<Book> getAllAvailableBooks() {
        return bookDAO.getAvailableBooks();
    }

    /**
     * Deletes a book from the library (Librarian feature).
     * @param bookId The ID of the book to delete.
     * @return true if deleted successfully, false otherwise.
     */
    public boolean deleteBook(int bookId) {
        // Before deleting, consider checking if there are any active loans for this book
        // This logic can be added here or in the DAO. For simplicity, assuming no active loans.
        return bookDAO.deleteBook(bookId);
    }

    /**
     * Gets a report of all overdue loans (Librarian feature).
     * @return A list of overdue Loan objects.
     */
    public List<Loan> getOverdueLoansReport() {
        LocalDate currentDate = LocalDate.now();
        return loanDAO.getOverdueLoans(currentDate);
    }
}
