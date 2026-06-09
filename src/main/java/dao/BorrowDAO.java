package dao;

import models.Borrow;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BorrowDAO {

    public void expireOldReservations() {
        String selectSql = "SELECT borrow_id, book_id FROM borrows WHERE status = 'RESERVED' AND reservation_expiry < NOW()";
        String deleteSql = "DELETE FROM borrows WHERE borrow_id = ?";
        String updateBookSql = "UPDATE books SET copies_available = copies_available + 1 WHERE book_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery()) {
             
            while (rs.next()) {
                int borrowId = rs.getInt("borrow_id");
                int bookId = rs.getInt("book_id");
                
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                     PreparedStatement updateStmt = conn.prepareStatement(updateBookSql)) {
                    
                    deleteStmt.setInt(1, borrowId);
                    if (deleteStmt.executeUpdate() > 0) {
                        updateStmt.setInt(1, bookId);
                        updateStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean markAsBorrowed(int borrowId) {
        String sql = "UPDATE borrows SET status = 'BORROWED', issue_date = CURDATE(), due_date = DATE_ADD(CURDATE(), INTERVAL 14 DAY), reservation_expiry = NULL WHERE borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, borrowId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean insertBorrow(Borrow borrow) {
        String sql = "INSERT INTO borrows (user_id, book_id, issue_date, due_date, status, reservation_expiry) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, borrow.getUserId());
            stmt.setInt(2, borrow.getBookId());
            stmt.setDate(3, borrow.getIssueDate());
            stmt.setDate(4, borrow.getDueDate());
            stmt.setString(5, borrow.getStatus());
            stmt.setTimestamp(6, borrow.getReservationExpiry());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        borrow.setBorrowId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public int getReservedBooksCountByUser(String userId) {
        expireOldReservations();
        String sql = "SELECT COUNT(*) FROM borrows WHERE user_id = ? AND status = 'RESERVED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getOverdueBooksCountByUser(String userId) {
        String sql = "SELECT COUNT(*) FROM borrows WHERE user_id = ? AND status = 'BORROWED' AND due_date < CURDATE() AND fine_paid = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Borrow> getBorrowsByUser(String userId, boolean onlyOverdue) {
        expireOldReservations();
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.book_id, b.issue_date, b.due_date, b.unreserve_requested, b.status, b.reservation_expiry, bk.title, bk.author " +
                     "FROM borrows b JOIN books bk ON b.book_id = bk.book_id " +
                     "WHERE b.user_id = ? AND b.status IN ('RESERVED', 'BORROWED')";
        if (onlyOverdue) {
            sql += " AND b.due_date < CURDATE() AND b.fine_paid = FALSE";
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Borrow borrow = new Borrow();
                    borrow.setBorrowId(rs.getInt("borrow_id"));
                    borrow.setBookId(rs.getInt("book_id"));
                    borrow.setIssueDate(rs.getDate("issue_date"));
                    borrow.setDueDate(rs.getDate("due_date"));
                    borrow.setUnreserveRequested(rs.getBoolean("unreserve_requested"));
                    borrow.setBookTitle(rs.getString("title"));
                    borrow.setBookAuthor(rs.getString("author"));
                    borrow.setStatus(rs.getString("status"));
                    borrow.setReservationExpiry(rs.getTimestamp("reservation_expiry"));
                    list.add(borrow);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Borrow> getStudentBorrows(String userId) {
        expireOldReservations();
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.book_id, b.issue_date, b.due_date, b.return_date, b.fine_paid, b.unreserve_requested, b.payment_method, b.status, b.reservation_expiry, b.fine_amount, " +
                     "bk.title, bk.author " +
                     "FROM borrows b JOIN books bk ON b.book_id = bk.book_id " +
                     "WHERE b.user_id = ? ORDER BY b.issue_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                long now = System.currentTimeMillis();
                while (rs.next()) {
                    Borrow borrow = new Borrow();
                    borrow.setBorrowId(rs.getInt("borrow_id"));
                    borrow.setBookId(rs.getInt("book_id"));
                    borrow.setIssueDate(rs.getDate("issue_date"));
                    borrow.setDueDate(rs.getDate("due_date"));
                    borrow.setReturnDate(rs.getDate("return_date"));
                    borrow.setFinePaid(rs.getBoolean("fine_paid"));
                    borrow.setUnreserveRequested(rs.getBoolean("unreserve_requested"));
                    borrow.setPaymentMethod(rs.getString("payment_method"));
                    borrow.setStatus(rs.getString("status"));
                    borrow.setReservationExpiry(rs.getTimestamp("reservation_expiry"));
                    borrow.setBookTitle(rs.getString("title"));
                    borrow.setBookAuthor(rs.getString("author"));

                    long dueTime = borrow.getDueDate().getTime();
                    long endTime = borrow.getReturnDate() != null ? borrow.getReturnDate().getTime() : now;
                    long diff = endTime - dueTime;
                    long overdue = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
                    if (overdue < 0) overdue = 0;
                    
                    borrow.setDaysOverdue(overdue);
                    borrow.setFineAmount(rs.getDouble("fine_amount") + (overdue * 1.0));
                    
                    list.add(borrow);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Borrow getBorrowById(int borrowId) {
        String sql = "SELECT b.borrow_id, b.user_id, b.book_id, b.issue_date, b.due_date, b.return_date, b.fine_paid, b.unreserve_requested, b.payment_method, b.status, b.reservation_expiry, b.fine_amount, " +
                     "bk.title, bk.author " +
                     "FROM borrows b JOIN books bk ON b.book_id = bk.book_id " +
                     "WHERE b.borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, borrowId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Borrow borrow = new Borrow();
                    borrow.setBorrowId(rs.getInt("borrow_id"));
                    borrow.setUserId(rs.getString("user_id"));
                    borrow.setBookId(rs.getInt("book_id"));
                    borrow.setIssueDate(rs.getDate("issue_date"));
                    borrow.setDueDate(rs.getDate("due_date"));
                    borrow.setReturnDate(rs.getDate("return_date"));
                    borrow.setFinePaid(rs.getBoolean("fine_paid"));
                    borrow.setUnreserveRequested(rs.getBoolean("unreserve_requested"));
                    borrow.setPaymentMethod(rs.getString("payment_method"));
                    borrow.setStatus(rs.getString("status"));
                    borrow.setReservationExpiry(rs.getTimestamp("reservation_expiry"));
                    borrow.setBookTitle(rs.getString("title"));
                    borrow.setBookAuthor(rs.getString("author"));

                    long now = System.currentTimeMillis();
                    long dueTime = borrow.getDueDate().getTime();
                    long endTime = borrow.getReturnDate() != null ? borrow.getReturnDate().getTime() : now;
                    long diff = endTime - dueTime;
                    long overdue = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
                    if (overdue < 0) overdue = 0;
                    
                    borrow.setDaysOverdue(overdue);
                    borrow.setFineAmount(rs.getDouble("fine_amount") + (overdue * 1.0));
                    return borrow;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getBorrowCountByUser(String userId) {
        String sql = "SELECT COUNT(*) FROM borrows WHERE user_id = ? AND status IN ('RESERVED', 'BORROWED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getBorrowedBooksCountOnlyByUser(String userId) {
        String sql = "SELECT COUNT(*) FROM borrows WHERE user_id = ? AND status = 'BORROWED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double getFineForUser(String userId) {
        double total = 0;
        String sql = "SELECT due_date, return_date, fine_amount FROM borrows WHERE user_id = ? AND fine_paid = FALSE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                long now = System.currentTimeMillis();
                while (rs.next()) {
                    long due = rs.getDate("due_date").getTime();
                    long endTime = rs.getDate("return_date") != null ? rs.getDate("return_date").getTime() : now;
                    long overdueDays = (long) Math.ceil((endTime - due) / (1000.0 * 60 * 60 * 24));
                    double dbFine = rs.getDouble("fine_amount");
                    if (overdueDays > 0) {
                        total += dbFine + (overdueDays * 1.0);
                    } else if (dbFine > 0) {
                        total += dbFine;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }

    public boolean returnBook(int borrowId, String userId) {
        // Get book_id and due_date before returning
        int bookId = -1;
        java.sql.Date dueDate = null;
        String selectSql = "SELECT book_id, due_date FROM borrows WHERE borrow_id = ? AND user_id = ? AND return_date IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setInt(1, borrowId);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    bookId = rs.getInt("book_id");
                    dueDate = rs.getDate("due_date");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        if (bookId == -1) return false;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Generate Fines
                String fineSql = "INSERT INTO fines (borrow_id, user_id, amount) SELECT borrow_id, user_id, fine_amount + (GREATEST(0, DATEDIFF(CURDATE(), due_date)) * 1) FROM borrows WHERE borrow_id = ? AND (fine_amount > 0 OR DATEDIFF(CURDATE(), due_date) > 0)";
                try (PreparedStatement fineStmt = conn.prepareStatement(fineSql)) {
                    fineStmt.setInt(1, borrowId);
                    fineStmt.executeUpdate();
                }

                // Update the borrow record
                String updateSql = "UPDATE borrows SET return_date = CURDATE(), status = 'RETURNED' WHERE borrow_id = ? AND user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, borrowId);
                    stmt.setString(2, userId);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        ReservationDAO resDao = new ReservationDAO();
                        boolean holdActivated = resDao.activateNextHold(bookId);
                        
                        if (!holdActivated) {
                            // Only restore copy if no holds are pending
                            String updateBooksSql = "UPDATE books SET copies_available = copies_available + 1 WHERE book_id = ?";
                            try (PreparedStatement stmt2 = conn.prepareStatement(updateBooksSql)) {
                                stmt2.setInt(1, bookId);
                                stmt2.executeUpdate();
                            }
                        }
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean payFine(int borrowId, String userId, String paymentMethod) {
        String sql = "UPDATE borrows SET fine_paid = TRUE, payment_method = ? WHERE borrow_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paymentMethod);
            stmt.setInt(2, borrowId);
            stmt.setString(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Borrow> getAllBorrows() {
        expireOldReservations();
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.user_id, b.book_id, b.issue_date, b.due_date, b.return_date, b.fine_paid, b.unreserve_requested, b.payment_method, b.status, b.reservation_expiry, b.fine_amount, " +
                     "bk.title, bk.author " +
                     "FROM borrows b JOIN books bk ON b.book_id = bk.book_id " +
                     "ORDER BY b.issue_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            long now = System.currentTimeMillis();
            while (rs.next()) {
                Borrow borrow = new Borrow();
                borrow.setBorrowId(rs.getInt("borrow_id"));
                borrow.setUserId(rs.getString("user_id"));
                borrow.setBookId(rs.getInt("book_id"));
                borrow.setIssueDate(rs.getDate("issue_date"));
                borrow.setDueDate(rs.getDate("due_date"));
                borrow.setReturnDate(rs.getDate("return_date"));
                borrow.setFinePaid(rs.getBoolean("fine_paid"));
                borrow.setUnreserveRequested(rs.getBoolean("unreserve_requested"));
                borrow.setPaymentMethod(rs.getString("payment_method"));
                borrow.setStatus(rs.getString("status"));
                borrow.setReservationExpiry(rs.getTimestamp("reservation_expiry"));
                borrow.setBookTitle(rs.getString("title"));
                borrow.setBookAuthor(rs.getString("author"));

                long dueTime = borrow.getDueDate().getTime();
                long endTime = borrow.getReturnDate() != null ? borrow.getReturnDate().getTime() : now;
                long diff = endTime - dueTime;
                long overdue = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
                if (overdue < 0) overdue = 0;
                
                borrow.setDaysOverdue(overdue);
                borrow.setFineAmount(rs.getDouble("fine_amount") + (overdue * 1.0));
                
                list.add(borrow);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean approveReturn(int borrowId) {
        int bookId = -1;
        String userId = null;
        java.sql.Date dueDate = null;
        String selectSql = "SELECT book_id, user_id, due_date FROM borrows WHERE borrow_id = ? AND return_date IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setInt(1, borrowId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    bookId = rs.getInt("book_id");
                    userId = rs.getString("user_id");
                    dueDate = rs.getDate("due_date");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        if (bookId == -1) return false;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Generate Fines
                String fineSql = "INSERT INTO fines (borrow_id, user_id, amount) SELECT borrow_id, user_id, fine_amount + (GREATEST(0, DATEDIFF(CURDATE(), due_date)) * 1) FROM borrows WHERE borrow_id = ? AND (fine_amount > 0 OR DATEDIFF(CURDATE(), due_date) > 0)";
                try (PreparedStatement fineStmt = conn.prepareStatement(fineSql)) {
                    fineStmt.setInt(1, borrowId);
                    fineStmt.executeUpdate();
                }

                String updateSql = "UPDATE borrows SET return_date = CURDATE(), status = 'RETURNED' WHERE borrow_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, borrowId);
                    if (stmt.executeUpdate() > 0) {
                        ReservationDAO resDao = new ReservationDAO();
                        boolean holdActivated = resDao.activateNextHold(bookId);

                        if (!holdActivated) {
                            String updateBooksSql = "UPDATE books SET copies_available = copies_available + 1 WHERE book_id = ?";
                            try (PreparedStatement stmt2 = conn.prepareStatement(updateBooksSql)) {
                                stmt2.setInt(1, bookId);
                                stmt2.executeUpdate();
                            }
                        }
                        conn.commit();
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean waiveFine(int borrowId) {
        String sql = "UPDATE borrows SET fine_paid = TRUE, payment_method = 'WAIVED' WHERE borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, borrowId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean requestCashPayment(int borrowId, String userId) {
        String sql = "UPDATE borrows SET payment_method = 'CASH' WHERE borrow_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, borrowId);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean requestUnreserve(int borrowId, String reason) {
        String sql = "UPDATE borrows SET unreserve_requested = TRUE, unreserve_reason = ? WHERE borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reason);
            stmt.setInt(2, borrowId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Borrow> getAllReservedBooks() {
        expireOldReservations();
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.user_id, b.book_id, b.issue_date, b.due_date, b.return_date, b.unreserve_requested, b.unreserve_reason, b.status, b.reservation_expiry, " +
                     "bk.title, bk.author, u.roll_no " +
                     "FROM borrows b " +
                     "JOIN books bk ON b.book_id = bk.book_id " +
                     "JOIN users u ON b.user_id = u.login_name " +
                     "WHERE b.status = 'RESERVED' " +
                     "ORDER BY b.reservation_expiry ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Borrow borrow = new Borrow();
                borrow.setBorrowId(rs.getInt("borrow_id"));
                borrow.setUserId(rs.getString("user_id"));
                borrow.setUserRollNo(rs.getString("roll_no"));
                borrow.setBookId(rs.getInt("book_id"));
                borrow.setIssueDate(rs.getDate("issue_date"));
                borrow.setDueDate(rs.getDate("due_date"));
                borrow.setReturnDate(rs.getDate("return_date"));
                borrow.setUnreserveRequested(rs.getBoolean("unreserve_requested"));
                borrow.setUnreserveReason(rs.getString("unreserve_reason"));
                borrow.setStatus(rs.getString("status"));
                borrow.setReservationExpiry(rs.getTimestamp("reservation_expiry"));
                borrow.setBookTitle(rs.getString("title"));
                borrow.setBookAuthor(rs.getString("author"));
                list.add(borrow);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Borrow> getAllUnreserveRequests() {
        List<Borrow> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.user_id, b.book_id, b.issue_date, b.due_date, b.return_date, b.unreserve_requested, b.unreserve_reason, " +
                     "bk.title, bk.author, u.roll_no " +
                     "FROM borrows b " +
                     "JOIN books bk ON b.book_id = bk.book_id " +
                     "JOIN users u ON b.user_id = u.login_name " +
                     "WHERE b.unreserve_requested = TRUE " +
                     "ORDER BY b.issue_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Borrow borrow = new Borrow();
                borrow.setBorrowId(rs.getInt("borrow_id"));
                borrow.setUserId(rs.getString("user_id"));
                borrow.setUserRollNo(rs.getString("roll_no"));
                borrow.setBookId(rs.getInt("book_id"));
                borrow.setIssueDate(rs.getDate("issue_date"));
                borrow.setDueDate(rs.getDate("due_date"));
                borrow.setReturnDate(rs.getDate("return_date"));
                borrow.setUnreserveRequested(rs.getBoolean("unreserve_requested"));
                borrow.setUnreserveReason(rs.getString("unreserve_reason"));
                borrow.setBookTitle(rs.getString("title"));
                borrow.setBookAuthor(rs.getString("author"));
                list.add(borrow);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean approveUnreserve(int borrowId) {
        int bookId = -1;
        String selectSql = "SELECT book_id FROM borrows WHERE borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setInt(1, borrowId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) bookId = rs.getInt("book_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        if (bookId == -1) return false;

        String deleteSql = "DELETE FROM borrows WHERE borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, borrowId);
            if (stmt.executeUpdate() > 0) {
                String updateBooksSql = "UPDATE books SET copies_available = copies_available + 1 WHERE book_id = ?";
                try (Connection conn2 = DBConnection.getConnection();
                     PreparedStatement stmt2 = conn2.prepareStatement(updateBooksSql)) {
                    stmt2.setInt(1, bookId);
                    stmt2.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Borrow getUnratedReturn(String userId) {
        String sql = "SELECT b.borrow_id, b.book_id, bk.title " +
                     "FROM borrows b JOIN books bk ON b.book_id = bk.book_id " +
                     "WHERE b.user_id = ? AND b.return_date IS NOT NULL AND b.rating_prompted = FALSE " +
                     "ORDER BY b.return_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Borrow borrow = new Borrow();
                    borrow.setBorrowId(rs.getInt("borrow_id"));
                    borrow.setBookId(rs.getInt("book_id"));
                    borrow.setBookTitle(rs.getString("title"));
                    return borrow;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean markRatingPrompted(int borrowId) {
        String sql = "UPDATE borrows SET rating_prompted = TRUE WHERE borrow_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, borrowId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<java.util.Map<String, Object>> getBorrowersForBook(int bookId) {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT b.borrow_id, b.issue_date, b.due_date, u.login_name, u.roll_no, u.mobile, u.email " +
                     "FROM borrows b JOIN users u ON b.user_id = u.login_name " +
                     "WHERE b.book_id = ? AND b.return_date IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("borrow_id", rs.getInt("borrow_id"));
                    map.put("issue_date", rs.getDate("issue_date"));
                    map.put("due_date", rs.getDate("due_date"));
                    map.put("login_name", rs.getString("login_name"));
                    map.put("roll_no", rs.getString("roll_no"));
                    map.put("mobile", rs.getString("mobile"));
                    map.put("email", rs.getString("email"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}

