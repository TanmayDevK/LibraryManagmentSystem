# Online Library Management System

Java Swing desktop app with MySQL (JDBC). Two roles: Librarian and Member.

## Features
**Librarian:** add / edit / delete books | create / edit / remove members | issue and return books |
send notifications (all or selected members) | inventory report with bar charts

**Member:** search books | borrow / return (max 3 at a time, 14-day loan) | borrowing history |
search history | due-date, new-arrival and announcement alerts (with preferences) | profile update

## Setup
1. Make sure MySQL is running, then load the database (from this folder):
   `mysql -u root -p < schema.sql`
2. Open `src/com/library/db/DBConnection.java` and put your MySQL password in `PASSWORD`.
3. In IntelliJ: File > Open > select `pom.xml` > Open as Project. Wait for Maven to download the MySQL driver.
4. Run `src/com/library/Main.java`.

**Demo logins:** librarian `admin` / `admin123`. Members register themselves from the login screen
(or the librarian creates them in the Members tab).

Already have an older database? Run `mysql -u root -p library_db < update_schema.sql` instead of `schema.sql`.

## Structure
```
src/com/library/
  Main.java
  model/      Book, User (abstract), Librarian, Member, LoanRecord, Notification, NotificationPrefs, SearchEntry
  db/         DBConnection, DbLock, CrudDAO, Searchable, BookDAO, UserDAO, TransactionDAO, NotificationDAO, SearchHistoryDAO
  exception/  LibraryException, BookNotAvailableException, BorrowLimitExceededException
  util/       OverdueMonitor, NotificationMonitor (background threads)
  ui/         LoginFrame, RegisterFrame, LibrarianDashboard, MemberDashboard, ReportsPanel, BarChartPanel,
              NotificationSenderPanel, MemberNotificationsPanel, SearchHistoryPanel
```
See `REVIEW1.md` for how the code maps to the Review 1 rubric.
