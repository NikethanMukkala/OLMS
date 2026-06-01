package utils;

import dao.BorrowDAO;
import models.Borrow;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("Starting diagnostic...");
        BorrowDAO dao = new BorrowDAO();

        int booksCount = dao.getReservedBooksCountByUser("testuser");
        System.out.println("Books Count via SQL: " + booksCount);

        // Perform a fake insertion
        System.out.println("Inserting test borrow...");
        Borrow borrow = new Borrow();
        borrow.setUserId("testuser");
        borrow.setBookId(1); // Assuming book 1 is The Great Gatsby
        borrow.setIssueDate(Date.valueOf(LocalDate.now()));
        borrow.setDueDate(Date.valueOf(LocalDate.now().plusDays(14)));
        boolean success = dao.insertBorrow(borrow);
        System.out.println("Insert Output: " + success);

        int updatedCount = dao.getReservedBooksCountByUser("testuser");
        System.out.println("New Books Count via SQL: " + updatedCount);

        List<Borrow> borrows = dao.getBorrowsByUser("testuser", false);
        System.out.println("Borrows list size: " + borrows.size());
        for(Borrow b : borrows) {
            System.out.println("- Borrow ID: " + b.getBorrowId() + " Name: " + b.getBookTitle());
        }
        
        System.out.println("End diagnostic.");
    }
}
