-- Database selection omitted for cloud compatibility

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    login_name VARCHAR(50) PRIMARY KEY,
    roll_no VARCHAR(20) UNIQUE,
    email VARCHAR(100) UNIQUE NOT NULL,
    mobile VARCHAR(10) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'LIBRARIAN', 'ADMIN') DEFAULT 'STUDENT',
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'APPROVED',
    is_active BOOLEAN DEFAULT TRUE
);
INSERT IGNORE INTO users (login_name, roll_no, email, mobile, password, role, status) VALUES ('admin', NULL, 'admin@library.com', '0000000000', 'admin', 'LIBRARIAN', 'APPROVED');

-- Books Table
CREATE TABLE IF NOT EXISTS books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(150) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    category VARCHAR(100),
    copies_total INT NOT NULL DEFAULT 0,
    copies_available INT NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Borrows Table
CREATE TABLE IF NOT EXISTS borrows (
    borrow_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    book_id INT,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    fine_amount DECIMAL(10,2) DEFAULT 0.00,
    return_date DATE DEFAULT NULL,
    fine_paid BOOLEAN DEFAULT FALSE,
    reminder_sent BOOLEAN DEFAULT FALSE,
    unreserve_requested BOOLEAN DEFAULT FALSE,
    unreserve_reason VARCHAR(255),
    rating_prompted BOOLEAN DEFAULT FALSE,
    status ENUM('RESERVED', 'BORROWED', 'RETURNED') DEFAULT 'RESERVED',
    reservation_expiry DATETIME DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(login_name),
    FOREIGN KEY (book_id) REFERENCES books(book_id)
);

-- Fines Table
CREATE TABLE IF NOT EXISTS fines (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id INT,
    user_id VARCHAR(50),
    amount DECIMAL(10,2) NOT NULL,
    paid_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrow_id) REFERENCES borrows(borrow_id),
    FOREIGN KEY (user_id) REFERENCES users(login_name)
);

-- Reservations Table
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT,
    user_id VARCHAR(50),
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'NOTIFIED', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    FOREIGN KEY (user_id) REFERENCES users(login_name),
    FOREIGN KEY (book_id) REFERENCES books(book_id)
);

-- Settings Table
CREATE TABLE IF NOT EXISTS settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255)
);
INSERT IGNORE INTO settings (setting_key, setting_value) VALUES 
('library_upi_id', 'library@ybl'), 
('library_upi_name', 'Central Library');

-- User Ratings Table
CREATE TABLE IF NOT EXISTS user_ratings (
    rating_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    book_id INT,
    rating INT CHECK(rating BETWEEN 1 AND 5),
    FOREIGN KEY (user_id) REFERENCES users(login_name),
    FOREIGN KEY (book_id) REFERENCES books(book_id)
);

-- User Recommendations Table
CREATE TABLE IF NOT EXISTS user_recommendations (
    recommendation_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    book_id INT,
    score DECIMAL(5,2),
    FOREIGN KEY (user_id) REFERENCES users(login_name),
    FOREIGN KEY (book_id) REFERENCES books(book_id)
);

-- Insert 50 Sample Books
DELETE FROM books;
INSERT INTO books (title, author, isbn, category, copies_total, copies_available) VALUES
('The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 'Fiction', 5, 5),
('To Kill a Mockingbird', 'Harper Lee', '9780060935467', 'Fiction', 10, 10),
('1984', 'George Orwell', '9780451524935', 'Science Fiction', 8, 8),
('Pride and Prejudice', 'Jane Austen', '9780141439518', 'Romance', 6, 6),
('The Catcher in the Rye', 'J.D. Salinger', '9780316769488', 'Fiction', 7, 7),
('The Hobbit', 'J.R.R. Tolkien', '9780345339683', 'Fantasy', 12, 12),
('Fahrenheit 451', 'Ray Bradbury', '9781451673319', 'Science Fiction', 5, 5),
('Moby-Dick', 'Herman Melville', '9780142437247', 'Adventure', 4, 4),
('Jane Eyre', 'Charlotte Brontë', '9780141441146', 'Romance', 6, 6),
('The Lord of the Rings', 'J.R.R. Tolkien', '9780544003415', 'Fantasy', 8, 8),
('Animal Farm', 'George Orwell', '9780451526342', 'Political Satire', 10, 10),
('Brave New World', 'Aldous Huxley', '9780060850524', 'Science Fiction', 7, 7),
('Wuthering Heights', 'Emily Brontë', '9780141439556', 'Romance', 5, 5),
('The Chronicles of Narnia', 'C.S. Lewis', '9780066238500', 'Fantasy', 9, 9),
('Frankenstein', 'Mary Shelley', '9780141439471', 'Horror', 4, 4),
('Dracula', 'Bram Stoker', '9780141439846', 'Horror', 6, 6),
('The Odyssey', 'Homer', '9780140268867', 'Epic', 5, 5),
('Crime and Punishment', 'Fyodor Dostoevsky', '9780140449136', 'Psychological Fiction', 4, 4),
('The Picture of Dorian Gray', 'Oscar Wilde', '9780141439570', 'Philosophical Fiction', 7, 7),
('The Alchemist', 'Paulo Coelho', '9780061122415', 'Adventure', 15, 15),
('Catch-22', 'Joseph Heller', '9780684833392', 'Satire', 6, 6),
('The Kite Runner', 'Khaled Hosseini', '9781594480003', 'Historical Fiction', 8, 8),
('Slaughterhouse-Five', 'Kurt Vonnegut', '9780385333849', 'Science Fiction', 5, 5),
('The Handmaid''s Tale', 'Margaret Atwood', '9780385490818', 'Dystopian', 9, 9),
('The Bell Jar', 'Sylvia Plath', '9780060837020', 'Semi-autobiographical', 6, 6),
('A Tale of Two Cities', 'Charles Dickens', '9780141439600', 'Historical Fiction', 7, 7),
('Don Quixote', 'Miguel de Cervantes', '9780142437209', 'Adventure', 4, 4),
('The Grapes of Wrath', 'John Steinbeck', '9780143039433', 'Historical Fiction', 6, 6),
('One Hundred Years of Solitude', 'Gabriel García Márquez', '9780060883287', 'Magical Realism', 8, 8),
('The Brothers Karamazov', 'Fyodor Dostoevsky', '9780374528379', 'Philosophical Fiction', 4, 4),
('Les Misérables', 'Victor Hugo', '9780451419439', 'Historical Fiction', 5, 5),
('Anna Karenina', 'Leo Tolstoy', '9780143035008', 'Romance', 4, 4),
('The Iliad', 'Homer', '9780140275360', 'Epic', 5, 5),
('The Count of Monte Cristo', 'Alexandre Dumas', '9780140449266', 'Adventure', 6, 6),
('Madame Bovary', 'Gustave Flaubert', '9780140449129', 'Literary Fiction', 5, 5),
('The Stranger', 'Albert Camus', '9780679720201', 'Philosophical Fiction', 10, 10),
('War and Peace', 'Leo Tolstoy', '9781400079988', 'Historical Fiction', 3, 3),
('Great Expectations', 'Charles Dickens', '9780141439563', 'Coming-of-age', 7, 7),
('Ulysses', 'James Joyce', '9780679722762', 'Modernist', 2, 2),
('Beloved', 'Toni Morrison', '9781400033416', 'Historical Fiction', 6, 6),
('Invisible Man', 'Ralph Ellison', '9780679732761', 'African American Literature', 5, 5),
('The Secret History', 'Donna Tartt', '9781400031702', 'Mystery', 7, 7),
('The Road', 'Cormac McCarthy', '9780307387899', 'Post-apocalyptic', 8, 8),
('Atonement', 'Ian McEwan', '9780385721790', 'Historical Fiction', 6, 6),
('The Color Purple', 'Alice Walker', '9780156028356', 'Epistolary Novel', 7, 7),
('Life of Pi', 'Yann Martel', '9780156027328', 'Adventure', 10, 10),
('The Book Thief', 'Markus Zusak', '9780375842207', 'Historical Fiction', 12, 12),
('A Game of Thrones', 'George R.R. Martin', '9780553593716', 'Fantasy', 15, 15),
('The Name of the Wind', 'Patrick Rothfuss', '9780756404741', 'Fantasy', 8, 8),
('Dune', 'Frank Herbert', '9780441172719', 'Science Fiction', 10, 10);
