package dao;

import models.Book;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public List<Book> listAvailableBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE copies_available > 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                books.add(extractBookFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public List<Book> searchBooks(String term, String category) {
        List<Book> books = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM books WHERE 1=1 ");
        
        boolean hasTerm = term != null && !term.trim().isEmpty();
        boolean hasCategory = category != null && !category.trim().isEmpty();

        if (hasTerm) {
            sql.append("AND (title LIKE ? OR author LIKE ?) ");
        }
        if (hasCategory) {
            sql.append("AND category = ? ");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
             
            int paramIndex = 1;
            if (hasTerm) {
                String searchPattern = "%" + term.trim() + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            if (hasCategory) {
                stmt.setString(paramIndex++, category.trim());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(extractBookFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public Book getBookDetails(String isbn) {
        Book book = null;
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, isbn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    book = extractBookFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return book;
    }
    
    public Book getBookById(int bookId) {
        Book book = null;
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    book = extractBookFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return book;
    }

    public boolean updateCopies(int bookId, int delta) {
        String sql = "UPDATE books SET copies_available = copies_available + ? WHERE book_id = ? AND (copies_available + ?) >= 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, delta);
            stmt.setInt(2, bookId);
            stmt.setInt(3, delta);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public int getTotalBooksCount() {
        String sql = "SELECT SUM(copies_total) FROM books";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getAvailableBooksCount() {
        String sql = "SELECT SUM(copies_available) FROM books";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM books ORDER BY category";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String cat = rs.getString("category");
                if (cat != null && !cat.trim().isEmpty()) {
                    categories.add(cat);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public boolean addBook(Book book) {
        String sql = "INSERT INTO books (title, author, isbn, category, copies_total, copies_available, image_url) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getIsbn());
            stmt.setString(4, book.getCategory());
            stmt.setInt(5, book.getCopiesTotal());
            stmt.setInt(6, book.getCopiesAvailable());
            stmt.setString(7, book.getImageUrl());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET title=?, author=?, isbn=?, category=?, copies_total=?, copies_available=?, image_url=? WHERE book_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getIsbn());
            stmt.setString(4, book.getCategory());
            stmt.setInt(5, book.getCopiesTotal());
            stmt.setInt(6, book.getCopiesAvailable());
            stmt.setString(7, book.getImageUrl());
            stmt.setInt(8, book.getBookId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteBook(int bookId) {
        try (Connection conn = DBConnection.getConnection()) {
            // First delete from child tables to prevent FK constraint failures
            conn.prepareStatement("DELETE FROM fines WHERE borrow_id IN (SELECT borrow_id FROM borrows WHERE book_id=" + bookId + ")").executeUpdate();
            conn.prepareStatement("DELETE FROM reservations WHERE book_id=" + bookId).executeUpdate();
            conn.prepareStatement("DELETE FROM user_ratings WHERE book_id=" + bookId).executeUpdate();
            conn.prepareStatement("DELETE FROM user_recommendations WHERE book_id=" + bookId).executeUpdate();
            conn.prepareStatement("DELETE FROM borrows WHERE book_id=" + bookId).executeUpdate();
            
            String sql = "DELETE FROM books WHERE book_id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, bookId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setIsbn(rs.getString("isbn"));
        book.setCategory(rs.getString("category"));
        book.setCopiesTotal(rs.getInt("copies_total"));
        book.setCopiesAvailable(rs.getInt("copies_available"));
        book.setImageUrl(rs.getString("image_url"));
        return book;
    }
}
