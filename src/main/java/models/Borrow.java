package models;

import java.sql.Date;

public class Borrow {
    private int borrowId;
    private String userId;
    private int bookId;
    private Date issueDate;
    private Date dueDate;
    private double fineAmount;
    private boolean reminderSent;
    private Date returnDate;
    private boolean finePaid;
    private long daysOverdue;
    private boolean unreserveRequested;
    private boolean ratingPrompted;
    private String unreserveReason;
    
    // Additional fields for joined queries
    private String bookTitle;
    private String bookAuthor;
    private String userRollNo;
    private String paymentMethod;
    
    private String status;
    private java.sql.Timestamp reservationExpiry;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.sql.Timestamp getReservationExpiry() { return reservationExpiry; }
    public void setReservationExpiry(java.sql.Timestamp reservationExpiry) { this.reservationExpiry = reservationExpiry; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getUnreserveReason() { return unreserveReason; }
    public void setUnreserveReason(String unreserveReason) { this.unreserveReason = unreserveReason; }

    public String getUserRollNo() { return userRollNo; }
    public void setUserRollNo(String userRollNo) { this.userRollNo = userRollNo; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookAuthor() { return bookAuthor; }
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    public int getBorrowId() { return borrowId; }
    public void setBorrowId(int borrowId) { this.borrowId = borrowId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public Date getIssueDate() { return issueDate; }
    public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    public boolean isFinePaid() { return finePaid; }
    public void setFinePaid(boolean finePaid) { this.finePaid = finePaid; }

    public long getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }

    public boolean isUnreserveRequested() { return unreserveRequested; }
    public void setUnreserveRequested(boolean unreserveRequested) { this.unreserveRequested = unreserveRequested; }

    public boolean isRatingPrompted() { return ratingPrompted; }
    public void setRatingPrompted(boolean ratingPrompted) { this.ratingPrompted = ratingPrompted; }
}
