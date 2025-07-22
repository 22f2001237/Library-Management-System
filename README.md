
# Library Management System

## Project Overview

This is a console-based Library Management System developed in Java. It utilizes JDBC (Java Database Connectivity) to interact with a MySQL database for persistent storage of library data. The system supports functionalities for both library members (users) and librarians, adhering to specific borrowing rules and fine calculations.

---

## Features

### User Actions

* **Borrow Book:** Members can borrow books, with a maximum limit of 4 books at a time.
* **Return Book:** Members can return borrowed books. Fines are calculated for overdue books.
* **Renew Book:** Members can renew a borrowed book once for an additional 3 days.
* **Check Book Availability:** Users can search for books by title, author, or ISBN and check their availability.
* **View Borrowed Books:** Members can see a list of books they currently have borrowed, along with their due dates and renewal status.
* **Check My Fine Details:** Members can view any accumulated fines for overdue books.

### Librarian Actions

* **Add New Book:** Librarians can add new books to the library's catalog.
* **Add New Member:** Librarians can register new members to the library system.
* **Check User Status:** Librarians can view a member's details, their currently borrowed books, return dates, and any outstanding fines.
* **View All Available Books:** Librarians can see a comprehensive list of all books currently available for borrowing.
* **View All Overdue Borrower Entries:** Librarians can generate a report of all overdue borrowed books.
* **Delete Book:** Librarians can remove books from the library's catalog.

### Business Rules

* **Maximum Borrow Limit:** A user can borrow a maximum of **4 books**.
* **Initial Borrow Period:** The maximum period for which a book can be borrowed is **5 days**.
* **Renewal Policy:** A user can **renew** each book **only once** for an additional **3 days**.
* **Overdue Fines:** Any delays in returning or renewing a book will incur an additional fine of **Rs. 10 per day**.

---

## Technologies Used

* **Java (JDK 8 or higher):** The core programming language.
* **JDBC (Java Database Connectivity):** API for connecting Java applications to databases.
* **MySQL Server:** The relational database management system used for data storage.
* **MySQL Connector/J:** The official JDBC driver for MySQL.

---

## Prerequisites

Before setting up and running the project, ensure you have the following installed:

1.  **Java Development Kit (JDK):** Version 8 or higher.
    * [Download JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
    * Verify installation: Open your terminal/command prompt and run `java -version` and `javac -version`.
2.  **MySQL Server:**
    * [Download MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
    * During installation, remember your `root` password.
    * Verify installation: Open your terminal/command prompt and run `mysql --version`.
3.  **MySQL Connector/J (JDBC Driver):**
    * [Download MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)
    * Download the "Platform Independent (ZIP)" version. You'll extract a `.jar` file (e.g., `mysql-connector-j-8.x.x.jar`).

---

## Database Setup

You need to create the database and tables in your MySQL server.

1.  **Connect to MySQL:**
    Open your terminal or command prompt and navigate to your MySQL `bin` directory (e.g., `C:\Program Files\MySQL\MySQL Server 8.0\bin` on Windows) or ensure MySQL is in your system's PATH. Then run:
    ```bash
    mysql -u root -p
    ```
    Enter your MySQL `root` password when prompted.

2.  **Create Database and User:**
    Execute the following SQL commands:
    ```sql
    CREATE DATABASE IF NOT EXISTS library_db;

    -- Create a user for your application (recommended for security)
    -- Replace 'your_secure_password' with a strong password
    CREATE USER 'library_user'@'localhost' IDENTIFIED BY 'your_secure_password';
    GRANT ALL PRIVILEGES ON library_db.* TO 'library_user'@'localhost';
    FLUSH PRIVILEGES;

    -- If using a central database for team collaboration, you might need a user
    -- that can connect from other IPs. Example:
    -- CREATE USER 'remote_library_user'@'%' IDENTIFIED BY 'your_secure_password_for_remote';
    -- GRANT ALL PRIVILEGES ON library_db.* TO 'remote_library_user'@'%';
    -- FLUSH PRIVILEGES;
    ```

3.  **Select Database:**
    ```sql
    USE library_db;
    ```

4.  **Create Tables:**
    Execute the following SQL to create the `books`, `members`, and `loans` tables:
    ```sql
    CREATE TABLE IF NOT EXISTS books (
        book_id INT AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        author VARCHAR(255) NOT NULL,
        isbn VARCHAR(20) UNIQUE NOT NULL,
        publication_year INT,
        total_copies INT DEFAULT 1,
        available_copies INT DEFAULT 1
    );

    CREATE TABLE IF NOT EXISTS members (
        member_id INT AUTO_INCREMENT PRIMARY KEY,
        first_name VARCHAR(100) NOT NULL,
        last_name VARCHAR(100) NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        phone_number VARCHAR(20),
        join_date DATE
    );

    CREATE TABLE IF NOT EXISTS loans (
        loan_id INT AUTO_INCREMENT PRIMARY KEY,
        book_id INT NOT NULL,
        member_id INT NOT NULL,
        loan_date DATE DEFAULT CURRENT_DATE,
        due_date DATE NOT NULL,
        return_date DATE,
        renewed BOOLEAN DEFAULT FALSE,
        FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
        FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
    );
    ```

5.  **Insert Sample Data (Optional):**
    You can insert some initial data to test the application:
    ```sql
    INSERT INTO books (title, author, isbn, publication_year, total_copies, available_copies) VALUES
    ('The Great Gatsby', 'F. Scott Fitzgerald', '978-0743273565', 1925, 5, 5),
    ('To Kill a Mockingbird', 'Harper Lee', '978-0061120084', 1960, 3, 3),
    ('1984', 'George Orwell', '978-0451524935', 1949, 7, 7),
    ('The Catcher in the Rye', 'J.D. Salinger', '978-0316769174', 1951, 4, 4);

    INSERT INTO members (first_name, last_name, email, phone_number, join_date) VALUES
    ('Alice', 'Wonder', 'alice.w@example.com', '111-222-3333', '2024-01-15'),
    ('Bob', 'Builder', 'bob.b@example.com', '444-555-6666', '2024-02-20'),
    ('Charlie', 'Chaplin', 'charlie.c@example.com', '777-888-9999', '2024-03-01');

    INSERT INTO loans (book_id, member_id, loan_date, due_date, renewed) VALUES
    (1, 1, '2024-07-15', '2024-07-20', FALSE);
    (2, 2, '2024-07-10', '2024-07-15', FALSE);
    ```
    Type `exit;` to leave the MySQL client.

---

## Project Structure

````

Library-Management-System/
├── src/
│   └── com/
│       └── library/
│           ├── LibraryApp.java         // Main application entry point (console UI)
│           ├── model/                 // Data model (POJOs) for database entities
│           │   ├── Book.java
│           │   ├── Member.java
│           │   └── Borrower.java      // Renamed from Loan.java
│           ├── dao/                   // Data Access Objects (JDBC interactions)
│           │   ├── DatabaseConnection.java
│           │   ├── BookDAO.java
│           │   ├── MemberDAO.java
│           │   └── BorrowerDAO.java   // Renamed from LoanDAO.java
│           └── service/               // Business Logic Layer
│               └── LibraryService.java
├── lib/                               // Directory for external JARs (e.g., MySQL Connector/J)
│   └── mysql-connector-j-8.x.x.jar
├── README.md                          // This file
├── .gitignore                         // Specifies files/folders to be ignored by Git
└── library\_db\_backup.sql              // (Optional) SQL dump of initial database schema and data

````

---
