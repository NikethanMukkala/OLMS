package dao;

import utils.DBConnection;
import utils.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    private static final Logger logger = LoggerFactory.getLogger(ReportDAO.class);

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDashboardStats() {
        String cacheKey = "dashboard_stats";
        Map<String, Object> cachedStats = (Map<String, Object>) CacheManager.globalStatsCache.getIfPresent(cacheKey);
        if (cachedStats != null) {
            return cachedStats;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", 0);
        stats.put("totalBorrowed", 0);
        stats.put("totalOverdue", 0);
        stats.put("totalRevenue", 0.0);

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT SUM(copies_total) FROM books");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalBooks", rs.getInt(1));
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM borrows WHERE return_date IS NULL");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalBorrowed", rs.getInt(1));
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM borrows WHERE return_date IS NULL AND due_date < CURDATE()");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalOverdue", rs.getInt(1));
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT SUM(amount) FROM fines WHERE paid_status = TRUE");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) stats.put("totalRevenue", rs.getDouble(1));
            }

            CacheManager.globalStatsCache.put(cacheKey, stats);

        } catch (SQLException e) {
            logger.error("Failed to compile dashboard stats: {}", e.getMessage(), e);
        }
        return stats;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPopularBooks(int limit) {
        String cacheKey = "popular_books_" + limit;
        List<Map<String, Object>> cachedList = (List<Map<String, Object>>) CacheManager.globalStatsCache.getIfPresent(cacheKey);
        if (cachedList != null) {
            return cachedList;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT bk.title, bk.author, COUNT(b.borrow_id) as borrow_count " +
                     "FROM books bk JOIN borrows b ON bk.book_id = b.book_id " +
                     "GROUP BY bk.book_id " +
                     "ORDER BY borrow_count DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("title", rs.getString("title"));
                    map.put("author", rs.getString("author"));
                    map.put("borrowCount", rs.getInt("borrow_count"));
                    list.add(map);
                }
                CacheManager.globalStatsCache.put(cacheKey, list);
            }
        } catch (SQLException e) {
             logger.error("Failed to compile popular books list: {}", e.getMessage(), e);
        }
        return list;
    }
}
