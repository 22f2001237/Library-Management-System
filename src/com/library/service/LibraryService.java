package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BorrowerDAO;
import com.library.dao.MemberDAO;
import com.library.dao.UserDAO;
import com.library.model.Book;
import com.library.model.Borrower;
import com.library.model.Member;
import com.library.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class LibraryService {

	private static final int MAX_BORROWED_BOOKS = 4;
	private static final int INITIAL_LOAN_DAYS = 5;
	private static final int RENEWAL_DAYS = 3;

	private BookDAO bookDAO;
	private MemberDAO memberDAO;
	private BorrowerDAO borrowerDAO;
	private UserDAO userDAO;

	public LibraryService(BookDAO bookDAO, MemberDAO memberDAO, BorrowerDAO borrowerDAO, UserDAO userDAO) {
		this.bookDAO = bookDAO;
		this.memberDAO = memberDAO;
		this.borrowerDAO = borrowerDAO;
		this.userDAO = userDAO;
	}

	// --- User Actions ---

	/**
	 * Allows a member to borrow a book.
	 * 
	 * @param memberId The ID of the member.
	 * @param bookId   The ID of the book.
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
			System.out.println("Error: Member " + member.getFirstName() + " has reached the maximum limit of "
					+ MAX_BORROWED_BOOKS + " borrowed books.");
			return false;
		}

		LocalDate loanDate = LocalDate.now();
		LocalDate dueDate = loanDate.plusDays(INITIAL_LOAN_DAYS);
		Borrower newBorrowerEntry = new Borrower(bookId, memberId, loanDate, dueDate);

		if (borrowerDAO.createLoan(newBorrowerEntry) != null) {
			bookDAO.updateBookCopies(bookId, -1); // Decrement available copies
			System.out
					.println("Book '" + book.getTitle() + "' borrowed successfully by " + member.getFirstName() + ".");
			System.out.println("Due date: " + dueDate);
			return true;
		}
		System.out.println("Failed to create borrower entry.");
		return false;
	}

	/**
	 * Allows a member to return a book.
	 * 
	 * @param loanId The ID of the loan (now borrower entry) to return.
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
			return borrowerEntry.calculateFine(borrowerEntry.getReturnDate());
		}

		LocalDate returnDate = LocalDate.now();
		double fine = borrowerEntry.calculateFine(returnDate);

		if (borrowerDAO.updateLoanReturnDate(loanId, returnDate)) {
			bookDAO.updateBookCopies(borrowerEntry.getBookId(), 1);
			System.out.println("Book for borrower entry ID " + loanId + " returned successfully.");
			if (fine > 0) {
				System.out.println("Fine for overdue: Rs. " + fine);
			} else {
				System.out.println("No fine incurred.");
			}
			return fine;
		}
		System.out.println("Failed to update borrower entry return date.");
		return 0.0;
	}

	/**
	 * Allows a member to renew a book.
	 * 
	 * @param loanId The ID of the loan (borrower entry) to renew.
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
			System.out.println(
					"Book for borrower entry ID " + loanId + " renewed successfully. New due date: " + newDueDate);
			return true;
		}
		System.out.println("Failed to renew book for borrower entry ID " + loanId + ".");
		return false;
	}

	/**
	 * Checks the availability of books based on a search query.
	 * 
	 * @param query The search string (title, author, or ISBN).
	 * @return A list of available books matching the query.
	 */
	public List<Book> checkBookAvailability(String query) {
		List<Book> foundBooks = bookDAO.searchBooks(query);
		return foundBooks.stream().filter(book -> book.getAvailableCopies() > 0).collect(Collectors.toList());
	}

	/**
	 * Gets all books currently borrowed by a specific member.
	 * 
	 * @param memberId The ID of the member.
	 * @return A list of Borrower objects representing borrowed books.
	 */
	public List<Borrower> getBorrowedBooks(int memberId) {
		return borrowerDAO.getActiveLoansByMemberId(memberId);
	}

	/**
	 * Calculates the total fine for a member based on their overdue loans.
	 * 
	 * @param memberId The ID of the member.
	 * @return The total fine amount.
	 */
	public double getMemberFineDetails(int memberId) {
		List<Borrower> activeLoans = borrowerDAO.getActiveLoansByMemberId(memberId);
		double totalFine = 0.0;
		LocalDate currentDate = LocalDate.now();

		System.out.println("\n--- Fine Details for Member ID: " + memberId + " ---");
		for (Borrower borrowerEntry : activeLoans) {
			double fine = borrowerEntry.calculateFine(currentDate);
			if (fine > 0) {
				Book book = bookDAO.getBookById(borrowerEntry.getBookId());
				System.out.println("  Borrower Entry ID: " + borrowerEntry.getLoanId() + ", Book: "
						+ (book != null ? book.getTitle() : "Unknown") + ", Due Date: " + borrowerEntry.getDueDate()
						+ ", Overdue Fine: Rs. " + fine);
				totalFine += fine;
			}
		}
		System.out.println("Total Fine: Rs. " + totalFine);
		return totalFine;
	}

	// --- Librarian Actions ---

	/**
	 * Adds a new book to the library catalog (Librarian feature).
	 * 
	 * @param book The Book object to add.
	 * @return true if added successfully, false otherwise.
	 */
	public boolean addBook(Book book) {
		return bookDAO.addBook(book) != null;
	}

	/**
	 * Adds a new member to the library system (Librarian feature).
	 * 
	 * @param member The Member object to add.
	 * @return true if added successfully, false otherwise.
	 */
	public boolean addMember(Member member) { // THIS IS THE NEW METHOD
		return memberDAO.addMember(member) != null;
	}

	/**
	 * Checks the status of a user (Librarian feature).
	 * 
	 * @param memberId The ID of the member.
	 */
	public void getMemberStatus(int memberId) {
		Member member = memberDAO.getMemberById(memberId);
		if (member == null) {
			System.out.println("Member with ID " + memberId + " not found.");
			return;
		}
		System.out.println("\n--- Member Status for: " + member.getFirstName() + " " + member.getLastName() + " (ID: "
				+ member.getMemberId() + ") ---");
		System.out.println("Email: " + member.getEmail());
		System.out.println("Phone: " + (member.getPhoneNumber() != null ? member.getPhoneNumber() : "N/A"));
		System.out.println("Joined: " + member.getJoinDate());

		List<Borrower> activeLoans = borrowerDAO.getActiveLoansByMemberId(memberId);
		System.out.println("\nBooks Borrowed (" + activeLoans.size() + "/" + MAX_BORROWED_BOOKS + "):");
		if (activeLoans.isEmpty()) {
			System.out.println("  No books currently borrowed.");
		} else {
			activeLoans.forEach(borrowerEntry -> {
				Book book = bookDAO.getBookById(borrowerEntry.getBookId());
				Member currentMember = memberDAO.getMemberById(borrowerEntry.getMemberId()); // Renamed variable to
																								// avoid conflict
				System.out.println("  - Borrower Entry ID: " + borrowerEntry.getLoanId() + ", Book: "
						+ (book != null ? book.getTitle() : "Unknown") + ", Due Date: " + borrowerEntry.getDueDate()
						+ ", Renewed: " + (borrowerEntry.isRenewed() ? "Yes" : "No"));
			});
		}
		getMemberFineDetails(memberId);
	}

	/**
	 * Retrieves all available books in the library (Librarian feature).
	 * 
	 * @return A list of available Book objects.
	 */
	public List<Book> getAllAvailableBooks() {
		return bookDAO.getAvailableBooks();
	}

	/**
	 * Deletes a book from the library (Librarian feature).
	 * 
	 * @param bookId The ID of the book to delete.
	 * @return true if deleted successfully, false otherwise.
	 */
	public boolean deleteBook(int bookId) {
		return bookDAO.deleteBook(bookId);
	}

	public boolean authenticateUser(String username, String password, String requiredRole) {
		User user = userDAO.getUserByUsername(username);
		if (user == null) {
			System.out.println("Authentication failed: User not found.");
			return false;
		}
		// In a real app, use password hashing (e.g., BCrypt) for secure comparison
		if (!user.getPassword().equals(password)) {
			System.out.println("Authentication failed: Incorrect password.");
			return false;
		}
		if (!user.getRole().equalsIgnoreCase(requiredRole)) {
			System.out.println("Authentication failed: Insufficient role. Expected '" + requiredRole + "'.");
			return false;
		}
		System.out.println("Authentication successful for " + username + " as " + requiredRole + ".");
		return true;
	}

	/**
	 * Gets a report of all overdue loans (Librarian feature).
	 * 
	 * @return A list of overdue Borrower objects.
	 */
	public List<Borrower> getOverdueLoansReport() {
		LocalDate currentDate = LocalDate.now();
		return borrowerDAO.getOverdueLoans(currentDate);
	}
}
