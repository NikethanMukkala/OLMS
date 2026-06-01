package dao;

import models.User;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> getAllStudents() {
        List<User> students = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'STUDENT'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setLoginName(rs.getString("login_name"));
                user.setRollNo(rs.getString("roll_no"));
                user.setEmail(rs.getString("email"));
                user.setMobile(rs.getString("mobile"));
                user.setRole(rs.getString("role"));
                user.setPending(false);
                students.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public List<User> getPendingStudents() {
        List<User> pending = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' AND status = 'PENDING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setLoginName(rs.getString("login_name"));
                user.setRollNo(rs.getString("roll_no"));
                user.setEmail(rs.getString("email"));
                user.setMobile(rs.getString("mobile"));
                user.setRole(rs.getString("role"));
                user.setPending(true);
                pending.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pending;
    }

    public boolean approveStudent(String loginName) {
        String sqlUpdate = "UPDATE users SET status = 'APPROVED' WHERE login_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
            stmt.setString(1, loginName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean rejectStudent(String loginName) {
        String sqlDelete = "DELETE FROM users WHERE login_name = ? AND status = 'PENDING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
            stmt.setString(1, loginName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public User getUserByRollNo(String rollNo) {
        String sql = "SELECT * FROM users WHERE roll_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rollNo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setLoginName(rs.getString("login_name"));
                    user.setRollNo(rs.getString("roll_no"));
                    user.setEmail(rs.getString("email"));
                    user.setMobile(rs.getString("mobile"));
                    user.setRole(rs.getString("role"));
                    user.setPending(false);
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verifyPassword(String loginName, String password) {
        String sql = "SELECT 1 FROM users WHERE login_name = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, loginName);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
