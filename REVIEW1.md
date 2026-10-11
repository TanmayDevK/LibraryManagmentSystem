# Online Library Management System (GUI-based project, Rubric A)

**Tech stack:** Java 17+, Swing (GUI), JDBC, MySQL, Maven, IntelliJ IDEA

**Description:** A desktop library application with two roles.
The **Librarian** manages books, members and loans, sends notifications and views inventory reports with charts.
The **Member** searches the catalogue, borrows and returns books (14-day loan, max 3 at a time), views borrowing
and search history, and receives alerts for due dates, new arrivals and announcements.

## Features

**Librarian:** add / edit / delete books | create / edit / remove members | issue and return books |
send notifications to all or selected members | inventory report with bar charts (copies by genre, loans per month)

**Member:** search books | borrow / return | borrowing history | search history ("search again") |
real-time notification alerts with preferences | profile update

## Rubric mapping (Review 1 - 33 marks)

| Rubric item | Marks | Where it is in the code |
|---|---|---|
| OOP (inheritance, polymorphism, exception handling, interfaces) | 10 | `model/User` (abstract) -> `Librarian`, `Member`; abstract methods `getBorrowLimit()`, `getDashboardTitle()` overridden and called through `User` references; interfaces `db/CrudDAO<T>`, `db/Searchable<T>` implemented by `BookDAO`; custom exceptions `LibraryException`, `BookNotAvailableException`, `BorrowLimitExceededException`; try / catch / finally and try-with-resources throughout the DAOs |
| Collections & Generics | 6 | `List<Book>`, `List<LoanRecord>`, `Map<String,Integer>` (TreeMap / LinkedHashMap) and `Set<String>` (TreeSet) in `ui/ReportsPanel`; `Comparator` sorting; `DefaultListModel<User>`, `JComboBox<Book>`; generic interfaces `CrudDAO<T>`, `Searchable<T>` |
| Multithreading & Synchronization | 4 | `util/OverdueMonitor` (librarian) and `util/NotificationMonitor` (member) run on background threads (`ScheduledExecutorService`) and update the GUI via `SwingUtilities.invokeLater`; `synchronized (DbLock.INSTANCE)` in `TransactionDAO` and `NotificationDAO` keeps the background threads and the GUI thread from interfering and stops two users taking the last copy |
| Classes for database operations (model + data access) | 7 | Models: `Book`, `User`, `Librarian`, `Member`, `LoanRecord`, `Notification`, `NotificationPrefs`, `SearchEntry`. DAOs: `BookDAO`, `UserDAO`, `TransactionDAO`, `NotificationDAO`, `SearchHistoryDAO`. Connection helper: `DBConnection` |
| Database connectivity (JDBC) | 3 | Full CRUD with `PreparedStatement` in every DAO |
| JDBC transaction management | 3 | `TransactionDAO.borrowBook` / `returnBook`: `setAutoCommit(false)`, `commit()`, `rollback()` on any failure |

## Database
Fresh install: run `schema.sql` (creates `library_db`, all tables, demo librarian `admin` / `admin123`, sample books).
Existing database: run `update_schema.sql` (adds the notifications, preferences and search-history tables).
