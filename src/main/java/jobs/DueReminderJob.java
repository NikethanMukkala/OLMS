package jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import utils.DBConnection;
import utils.EmailUtility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DueReminderJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Executing DueReminderJob...");
        
        String sql = "SELECT b.borrow_id, bk.title, b.due_date, u.email, u.login_name " +
                     "FROM borrows b " +
                     "JOIN books bk ON b.book_id = bk.book_id " +
                     "JOIN users u ON b.user_id = u.login_name " +
                     "WHERE b.return_date IS NULL AND b.reminder_sent = FALSE AND DATEDIFF(b.due_date, CURDATE()) = 1";
                     
        String updateSql = "UPDATE borrows SET reminder_sent = TRUE WHERE borrow_id = ?";
                     
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
             while (rs.next()) {
                 int borrowId = rs.getInt("borrow_id");
                 String title = rs.getString("title");
                 String dueDate = rs.getDate("due_date").toString();
                 String email = rs.getString("email");
                 String name = rs.getString("login_name");
                 
                 // Send Email
                 String subject = "Automated Reminder: Book Due Tomorrow";
                 String body = String.format("Dear %s,\n\nThis is a friendly reminder that the book '%s' is due tomorrow (%s).\n" +
                                             "Please ensure it is returned on time to avoid a late fine of ₹10 per day.\n\n" +
                                             "Thank you,\nLibrary Management System", name, title, dueDate);
                 
                 EmailUtility.sendEmail(email, subject, body);
                 
                 // Update flag
                 try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                     updateStmt.setInt(1, borrowId);
                     updateStmt.executeUpdate();
                 }
             }
        } catch (SQLException e) {
            System.err.println("Error executing DueReminderJob: " + e.getMessage());
        }
    }
}
