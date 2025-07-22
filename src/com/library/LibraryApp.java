package com.library;

import com.library.dao.BookDAO;
import com.library.dao.BorrowerDAO;
import com.library.dao.MemberDAO;
import com.library.model.Book;
import com.library.model.Borrower;
import com.library.model.Member;
import com.library.service.LibraryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class LibraryApp {

    private static BookDAO bookDAO;
    private static MemberDAO memberDAO;
    private static BorrowerDAO borrowerDAO;
    private static LibraryService libraryService;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        bookDAO = new BookDAO();
        memberDAO = new MemberDAO();
        borrowerDAO = new BorrowerDAO();
        libraryService = new LibraryService(bookDAO, memberDAO, borrowerDAO);

        while (true) {
            printMainMenu();
            int choice = getUserChoice();

            switch (choice) {
                case 1:
                    userMenu();
                    break;
                case 2:
                    librarianMenu();
                    break;
                case 3:
                    System.out.println("Exiting Library System. Goodbye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n--- Library System ---");
        System.out.println("1. User Actions");
        System.out.println("2. Librarian Actions");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }

    private static int getUserChoice() {
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.next(); // Consume invalid input
            System.out.print("Enter your choice: ");
        }
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        return choice;
    }

    private static void userMenu() {
        while (true) {
            System.out.println("\n--- User Menu ---");
            System.out.println("1. Borrow Book");
            System.out.println("2. Return Book");
            System.out.println("3. Renew Book");
            System.out.println("4. Check Book Availability");
            System.out.println("5. View Borrowed Books");
            System.out.println("6. Check My Fine Details");
            System.out.println("7. Back to Main Menu");
            System.out.print("Enter your choice: ");
            int choice = getUserChoice();

            switch (choice) {
                case 1:
                    System.out.print("Enter Member ID: ");
                    int memberIdBorrow = getUserChoice();
                    System.out.print("Enter Book ID to borrow: ");
                    int bookIdBorrow = getUserChoice();
                    libraryService.borrowBook(memberIdBorrow, bookIdBorrow);
                    break;
                case 2:
                    System.out.print("Enter Borrower Entry ID to return: ");
                    int loanIdReturn = getUserChoice();
                    libraryService.returnBook(loanIdReturn);
                    break;
                case 3:
                    System.out.print("Enter Borrower Entry ID to renew: ");
                    int loanIdRenew = getUserChoice();
                    libraryService.renewBook(loanIdRenew);
                    break;
                case 4:
                    System.out.print("Enter search query (title, author, ISBN): ");
                    String searchQuery = scanner.nextLine();
                    List<Book> availableBooks = libraryService.checkBookAvailability(searchQuery);
                    if (availableBooks.isEmpty()) {
                        System.out.println("No available books found matching your query.");
                    } else {
                        System.out.println("\n--- Available Books ---");
                        availableBooks.forEach(System.out::println);
                    }
                    break;
                case 5:
                    System.out.print("Enter your Member ID: ");
                    int memberIdView = getUserChoice();
                    List<Borrower> borrowedBooks = libraryService.getBorrowedBooks(memberIdView);
                    if (borrowedBooks.isEmpty()) {
                        System.out.println("You have no books currently borrowed.");
                    } else {
                        System.out.println("\n--- Your Borrowed Books ---");
                        borrowedBooks.forEach(borrowerEntry -> {
                            Book book = bookDAO.getBookById(borrowerEntry.getBookId());
                            System.out.println("Borrower Entry ID: " + borrowerEntry.getLoanId() +
                                               ", Book: " + (book != null ? book.getTitle() : "Unknown") +
                                               ", Due Date: " + borrowerEntry.getDueDate() +
                                               ", Renewed: " + (borrowerEntry.isRenewed() ? "Yes" : "No"));
                        });
                    }
                    break;
                case 6:
                    System.out.print("Enter your Member ID: ");
                    int memberIdFine = getUserChoice();
                    libraryService.getMemberFineDetails(memberIdFine);
                    break;
                case 7:
                    return; // Back to main menu
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void librarianMenu() {
        while (true) {
            System.out.println("\n--- Librarian Menu ---");
            System.out.println("1. Add New Book");
            System.out.println("2. Add New Member"); 
            System.out.println("3. Check User Status");
            System.out.println("4. View All Available Books");
            System.out.println("5. View All Overdue Borrower Entries");
            System.out.println("6. Delete Book");
            System.out.println("7. Back to Main Menu"); 
            System.out.print("Enter your choice: ");
            int choice = getUserChoice();

            switch (choice) {
                case 1:
                    System.out.println("--- Add New Book ---");
                    System.out.print("Title: ");
                    String title = scanner.nextLine();
                    System.out.print("Author: ");
                    String author = scanner.nextLine();
                    System.out.print("ISBN: ");
                    String isbn = scanner.nextLine();
                    System.out.print("Publication Year: ");
                    int pubYear = getUserChoice();
                    System.out.print("Total Copies: ");
                    int totalCopies = getUserChoice();
                    Book newBook = new Book(title, author, isbn, pubYear, totalCopies, totalCopies);
                    libraryService.addBook(newBook);
                    break;
                case 2: 
                    System.out.println("--- Add New Member ---");
                    System.out.print("First Name: ");
                    String firstName = scanner.nextLine();
                    System.out.print("Last Name: ");
                    String lastName = scanner.nextLine();
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    System.out.print("Phone Number (optional, press Enter to skip): ");
                    String phoneNumber = scanner.nextLine();
                    Member newMember = new Member(firstName, lastName, email, phoneNumber.isEmpty() ? null : phoneNumber, LocalDate.now());
                    if (libraryService.addMember(newMember)) { // Call the new addMember method in LibraryService
                        System.out.println("Member added successfully! Member ID: " + newMember.getMemberId());
                    } else {
                        System.out.println("Failed to add member. Email might already exist.");
                    }
                    break;
                case 3: 
                    System.out.print("Enter Member ID to check status: ");
                    int memberIdStatus = getUserChoice();
                    libraryService.getMemberStatus(memberIdStatus);
                    break;
                case 4: 
                    List<Book> allAvailableBooks = libraryService.getAllAvailableBooks();
                    if (allAvailableBooks.isEmpty()) {
                        System.out.println("No books currently available in the library.");
                    } else {
                        System.out.println("\n--- All Available Books ---");
                        allAvailableBooks.forEach(System.out::println);
                    }
                    break;
                case 5: 
                    List<Borrower> overdueLoans = libraryService.getOverdueLoansReport();
                    if (overdueLoans.isEmpty()) {
                        System.out.println("No overdue borrower entries found.");
                    } else {
                        System.out.println("\n--- Overdue Borrower Entries Report ---");
                        overdueLoans.forEach(borrowerEntry -> {
                            Book book = bookDAO.getBookById(borrowerEntry.getBookId());
                            Member member = memberDAO.getMemberById(borrowerEntry.getMemberId());
                            System.out.println("Borrower Entry ID: " + borrowerEntry.getLoanId() +
                                               ", Book: " + (book != null ? book.getTitle() : "Unknown") +
                                               ", Member: " + (member != null ? member.getFirstName() + " " + member.getLastName() : "Unknown") +
                                               ", Due Date: " + borrowerEntry.getDueDate() +
                                               ", Fine: Rs. " + borrowerEntry.calculateFine(LocalDate.now()));
                        });
                    }
                    break;
                case 6: 
                    System.out.print("Enter Book ID to delete: ");
                    int bookIdDelete = getUserChoice();
                    if (libraryService.deleteBook(bookIdDelete)) {
                        System.out.println("Book deleted successfully.");
                    } else {
                        System.out.println("Failed to delete book. It might not exist or has active loans.");
                    }
                    break;
                case 7: 
                    return; 
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
