-- Online Library Management System - full schema (fresh install)
-- If you already ran an older schema.sql, run update_schema.sql instead.
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role ENUM('LIBRARIAN', 'MEMBER') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    author VARCHAR(100) NOT NULL,
    isbn VARCHAR(30),
    genre VARCHAR(50),
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    member_id INT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status ENUM('BORROWED', 'RETURNED') NOT NULL DEFAULT 'BORROWED',
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (member_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    type ENUM('ANNOUNCEMENT', 'DUE_DATE', 'NEW_ARRIVAL') NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification_prefs (
    member_id INT PRIMARY KEY,
    due_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    new_arrival_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    announcements BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS search_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    keyword VARCHAR(150) NOT NULL,
    searched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Demo librarian: username admin / password admin123
INSERT INTO users (username, password, full_name, email, role)
VALUES ('admin', 'admin123', 'Head Librarian', 'admin@library.local', 'LIBRARIAN')
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO books (title, author, isbn, genre, total_copies, available_copies) VALUES
('Clean Code', 'Robert C. Martin', '9780132350884', 'Programming', 3, 3),
('The Alchemist', 'Paulo Coelho', '9780062315007', 'Fiction', 2, 2),
('A Brief History of Time', 'Stephen Hawking', '9780553380163', 'Science', 2, 2),
('Wings of Fire', 'A.P.J. Abdul Kalam', '9788173711466', 'Biography', 4, 4);
