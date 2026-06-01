package servlets;

import dao.BookDAO;
import dao.BorrowDAO;
import models.Book;
import models.Borrow;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet("/books")
public class BookServlet extends HttpServlet {
    private BookDAO bookDAO;
    private BorrowDAO borrowDAO;

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        borrowDAO = new BorrowDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null || !"LIBRARIAN".equals(session.getAttribute("role"))) {
            response.sendRedirect("login.jsp");
            return;
        }

        String action = request.getParameter("action");
        if ("add".equals(action)) {
            Book book = new Book();
            book.setTitle(request.getParameter("title"));
            book.setAuthor(request.getParameter("author"));
            book.setIsbn(request.getParameter("isbn"));
            book.setCategory(request.getParameter("category"));
            book.setImageUrl(request.getParameter("imageUrl"));
            int copies = Integer.parseInt(request.getParameter("copies"));
            book.setCopiesTotal(copies);
            book.setCopiesAvailable(copies); // Available equals total for new book
            
            boolean added = bookDAO.addBook(book);
            if (added) request.getSession().setAttribute("success", "Book added successfully.");
            else request.getSession().setAttribute("error", "Failed to add book.");
            
        } else if ("update".equals(action)) {
            Book book = new Book();
            book.setBookId(Integer.parseInt(request.getParameter("book_id")));
            book.setTitle(request.getParameter("title"));
            book.setAuthor(request.getParameter("author"));
            book.setIsbn(request.getParameter("isbn"));
            book.setCategory(request.getParameter("category"));
            book.setImageUrl(request.getParameter("imageUrl"));
            book.setCopiesTotal(Integer.parseInt(request.getParameter("copies_total")));
            book.setCopiesAvailable(Integer.parseInt(request.getParameter("copies_available")));
            
            boolean updated = bookDAO.updateBook(book);
            if (updated) request.getSession().setAttribute("success", "Book updated successfully.");
            else request.getSession().setAttribute("error", "Failed to update book.");
            
        } else if ("delete".equals(action)) {
            int bookId = Integer.parseInt(request.getParameter("book_id"));
            boolean deleted = bookDAO.deleteBook(bookId);
            if (deleted) request.getSession().setAttribute("success", "Book deleted successfully.");
            else request.getSession().setAttribute("error", "Failed to delete book.");
        }
        
        response.sendRedirect("books?view=books");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        
        String role = (String) session.getAttribute("role");
        if (!"STUDENT".equals(role) && !"LIBRARIAN".equals(role)) {
            response.sendRedirect("login.jsp");
            return;
        }

        String userId = (String) session.getAttribute("user_id");
        String search = request.getParameter("search");
        String category = request.getParameter("category");

        String view = request.getParameter("view");

        if ("LIBRARIAN".equals(role)) {
            if ("book_details".equals(view)) {
                int bookId = Integer.parseInt(request.getParameter("bookId"));
                Book book = bookDAO.getBookById(bookId);
                List<java.util.Map<String, Object>> borrowers = borrowDAO.getBorrowersForBook(bookId);
                request.setAttribute("book", book);
                request.setAttribute("borrowers", borrowers);
                request.setAttribute("view", "book_details");
                request.getRequestDispatcher("librarian-dashboard.jsp").forward(request, response);
                return;
            }

            List<Book> books;
            if ((search != null && !search.trim().isEmpty()) || (category != null && !category.trim().isEmpty())) {
                books = bookDAO.searchBooks(search, category);
            } else {
                books = bookDAO.searchBooks("", ""); 
            }
            request.setAttribute("books", books);
            request.setAttribute("view", "books");
            request.getRequestDispatcher("librarian-dashboard.jsp").forward(request, response);
            return;
        }

        if ("reservations".equals(view)) {
            List<Borrow> myBorrows = borrowDAO.getBorrowsByUser(userId, false);
            request.setAttribute("myBorrows", myBorrows);
            request.setAttribute("viewType", "reservations");
        } else if ("history".equals(view)) {
            List<Borrow> myBorrows = borrowDAO.getStudentBorrows(userId);
            request.setAttribute("myBorrows", myBorrows);
            request.setAttribute("viewType", "history");
        } else if ("dues".equals(view)) {
            List<Borrow> myBorrows = borrowDAO.getStudentBorrows(userId);
            request.setAttribute("myBorrows", myBorrows);
            request.setAttribute("viewType", "dues");
        } else {
            List<Book> books;
            if ((search != null && !search.trim().isEmpty()) || (category != null && !category.trim().isEmpty())) {
                books = bookDAO.searchBooks(search, category);
            } else {
                books = bookDAO.listAvailableBooks(); // Default: show available books
            }
            request.setAttribute("books", books);
            request.setAttribute("viewType", "books");
        }

        int totalBooks = bookDAO.getTotalBooksCount();
        int availableBooks = bookDAO.getAvailableBooksCount();
        int reservedByUser = borrowDAO.getReservedBooksCountByUser(userId);
        int overdueByUser = borrowDAO.getOverdueBooksCountByUser(userId);
        List<String> categories = bookDAO.getAllCategories();

        List<Borrow> allMyActiveBorrows = borrowDAO.getBorrowsByUser(userId, false);
        request.setAttribute("allMyActiveBorrows", allMyActiveBorrows);

        request.setAttribute("totalBooks", totalBooks);
        request.setAttribute("availableBooks", availableBooks);
        request.setAttribute("reservedByUser", reservedByUser);
        request.setAttribute("overdueByUser", overdueByUser);
        request.setAttribute("categories", categories);
        
        // Preserve search inputs
        request.setAttribute("searchParam", search);
        request.setAttribute("categoryParam", category);

        request.getRequestDispatcher("student-dashboard.jsp").forward(request, response);
    }
}
