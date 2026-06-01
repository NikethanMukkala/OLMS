<%@ page import="java.sql.*, utils.DBConnection" %>
<%
try (Connection conn = DBConnection.getConnection()) {
    PreparedStatement ps = conn.prepareStatement("SELECT login_name, role, status FROM users");
    ResultSet rs = ps.executeQuery();
    int count = 0;
    while (rs.next()) {
        count++;
        out.println(rs.getString("login_name") + " | " + rs.getString("role") + " | " + rs.getString("status") + "\n");
    }
    if (count == 0) out.println("EMPTY_TABLE\n");
} catch(Exception e) {
    out.println("ERROR: " + e.getMessage() + "\n");
}
%>
