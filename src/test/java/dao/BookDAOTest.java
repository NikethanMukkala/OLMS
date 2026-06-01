package dao;

import models.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class BookDAOTest {

    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockPreparedStatement;
    @Mock
    private ResultSet mockResultSet;

    private MockedStatic<DBConnection> mockedDbConnection;
    private BookDAO bookDAO;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        bookDAO = new BookDAO();

        // Intercept static DBConnection.getConnection() bypassing native SQL DB instances mapped
        mockedDbConnection = mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);
        
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    public void tearDown() {
        mockedDbConnection.close();
    }

    @Test
    public void testGetBookDetailsFound() throws Exception {
        String testIsbn = "1234567890";
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("book_id")).thenReturn(1);
        when(mockResultSet.getString("title")).thenReturn("Effective Java");
        when(mockResultSet.getString("author")).thenReturn("Joshua Bloch");
        when(mockResultSet.getString("isbn")).thenReturn(testIsbn);
        when(mockResultSet.getString("category")).thenReturn("Programming");
        when(mockResultSet.getInt("copies_total")).thenReturn(5);
        when(mockResultSet.getInt("copies_available")).thenReturn(3);

        Book result = bookDAO.getBookDetails(testIsbn);

        assertNotNull(result);
        assertEquals("Effective Java", result.getTitle());
        verify(mockPreparedStatement).setString(1, testIsbn);
    }

    @Test
    public void testGetBookDetailsNotFound() throws Exception {
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Book result = bookDAO.getBookDetails("invalid_isbn");

        assertNull(result);
    }
}
