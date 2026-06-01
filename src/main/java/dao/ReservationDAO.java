package dao;

import utils.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ReservationDAO {

    public boolean placeHold(int bookId, String userId) {
        // Check if already reserved by this user
        String checkSql = "SELECT * FROM reservations WHERE book_id=? AND user_id=? AND status='PENDING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, bookId);
            checkStmt.setString(2, userId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) return false; // Already queued
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        String sql = "INSERT INTO reservations (book_id, user_id, status) VALUES (?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean activateNextHold(int bookId) {
        String sql = "SELECT reservation_id, user_id FROM reservations WHERE book_id=? AND status='PENDING' ORDER BY request_date ASC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int resId = rs.getInt("reservation_id");
                    String updateSql = "UPDATE reservations SET status='NOTIFIED' WHERE reservation_id=?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, resId);
                        updateStmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
