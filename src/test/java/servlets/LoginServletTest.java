package servlets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import utils.DBConnection;
import utils.HashUtil;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mockito.Mockito.*;

public class LoginServletTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher requestDispatcher;
    
    @Mock private Connection mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private ResultSet mockResultSet;

    private MockedStatic<DBConnection> mockedDbConnection;
    private MockedStatic<HashUtil> mockedHashUtil;

    @InjectMocks
    private LoginServlet loginServlet;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
        
        mockedHashUtil = mockStatic(HashUtil.class);

        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher("login.jsp")).thenReturn(requestDispatcher);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @AfterEach
    public void tearDown() {
        mockedDbConnection.close();
        mockedHashUtil.close();
    }

    @Test
    public void testStudentLoginSuccess() throws Exception {
        when(request.getParameter("role")).thenReturn("STUDENT");
        when(request.getParameter("roll_no")).thenReturn("101");
        when(request.getParameter("password")).thenReturn("secret!");

        // Mock DB returning a stored hash
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("password")).thenReturn("hashed_secret");
        when(mockResultSet.getString("login_name")).thenReturn("John Doe");

        // Mock Bcrypt validation
        mockedHashUtil.when(() -> HashUtil.checkPassword("secret!", "hashed_secret")).thenReturn(true);

        loginServlet.doPost(request, response);

        verify(session).setAttribute(eq("user_id"), eq("John Doe"));
        verify(session).setAttribute(eq("role"), eq("STUDENT"));
        verify(response).sendRedirect("student-dashboard.jsp");
    }

    @Test
    public void testStudentLoginFailureWrongPassword() throws Exception {
        when(request.getParameter("role")).thenReturn("STUDENT");
        when(request.getParameter("roll_no")).thenReturn("101");
        when(request.getParameter("password")).thenReturn("wrong!");

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("password")).thenReturn("hashed_secret");

        // BCrypt validation explicitly failing modeled
        mockedHashUtil.when(() -> HashUtil.checkPassword("wrong!", "hashed_secret")).thenReturn(false);

        loginServlet.doPost(request, response);

        verify(request).setAttribute(eq("error"), anyString());
        verify(requestDispatcher).forward(request, response);
        verify(response, never()).sendRedirect(anyString());
    }
}
