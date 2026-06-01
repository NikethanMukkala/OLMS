<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, models.*" %>
<%
    String role = (String) session.getAttribute("role");
    if (role == null || !"LIBRARIAN".equals(role)) {
        response.sendRedirect("login.jsp");
        return;
    }
    String view = (String) request.getAttribute("view");
    if (view == null) view = "dashboard";
    
    String successMsg = (String) request.getAttribute("success");
    if (successMsg == null) successMsg = (String) session.getAttribute("success");
    String errorMsg = (String) request.getAttribute("error");
    if (errorMsg == null) errorMsg = (String) session.getAttribute("error");
    
    session.removeAttribute("success");
    session.removeAttribute("error");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Librarian Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <style>
        .dashboard-container { display: flex; min-height: calc(100vh - 70px); }
        .sidebar { width: 260px; background: var(--card-bg); border-right: 1px solid var(--border-color); padding: 24px 20px; display: flex; flex-direction: column; gap: 8px; z-index: 10; font-weight: 500;}
        .sidebar a { display: flex; align-items: center; padding: 12px 16px; color: var(--text-secondary); text-decoration: none; border-radius: 10px; transition: all 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
        .sidebar a:hover { background: rgba(67, 97, 238, 0.08); color: var(--primary-color); transform: translateX(6px); }
        .sidebar a.active { background: linear-gradient(135deg, var(--primary-color) 0%, #818cf8 100%); color: white; box-shadow: 0 4px 12px rgba(67, 97, 238, 0.3); }
        
        .main-content { flex: 1; padding: 32px 40px; overflow-y: auto; background: var(--background-color); }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 24px; margin-bottom: 40px; }
        .stat-card { background: var(--card-bg); padding: 24px; border-radius: 16px; border: 1px solid var(--border-color); box-shadow: var(--shadow-sm); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); transition: all 0.3s ease; position: relative; overflow: hidden; }
        .stat-card::before { content: ''; position: absolute; left: 0; top: 0; height: 100%; width: 4px; background: var(--primary-color); border-radius: 4px 0 0 4px; transition: width 0.3s ease; opacity: 0.8; }
        .stat-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-md); }
        .stat-card:hover::before { width: 100%; opacity: 0.04; }
        .stat-card h3 { margin: 0; color: var(--text-secondary); font-size: 14px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; position: relative; z-index: 1;}
        .stat-card p { margin: 12px 0 0; font-size: 32px; font-weight: 800; color: var(--text-primary); position: relative; z-index: 1;}
        
        table { width: 100%; border-collapse: separate; border-spacing: 0; background: var(--card-bg); border-radius: 16px; border: 1px solid var(--border-color); box-shadow: var(--shadow-sm); margin: 20px 0; overflow: hidden; backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
        th, td { padding: 16px 20px; text-align: left; border-bottom: 1px solid var(--border-color); }
        th { background: rgba(0,0,0,0.03); font-weight: 600; color: var(--text-secondary); font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: rgba(0,0,0,0.02); }
        
        .badge { padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; letter-spacing: 0.3px; display: inline-block; }
        .badge-pending { background: rgba(245, 158, 11, 0.15); color: #d97706; border: 1px solid rgba(245, 158, 11, 0.2); }
        .badge-approved { background: rgba(16, 185, 129, 0.15); color: #059669; border: 1px solid rgba(16, 185, 129, 0.2); }
        
        .navbar { display: flex; justify-content: space-between; align-items: center; padding: 16px 32px; background: var(--card-bg); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border-bottom: 1px solid var(--border-color); position: sticky; top: 0; z-index: 100; box-shadow: var(--shadow-sm); }
        .navbar-brand { font-size: 20px; font-weight: 700; color: var(--text-primary); text-decoration: none; }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="librarian-dashboard" class="navbar-brand">📚 Librarian Desk</a>
        <div class="nav-links">
            <span style="margin-right: 15px; color: var(--text-secondary);">Admin: <%= session.getAttribute("user_id") %></span>
            <a href="logout" class="btn btn-secondary" style="padding: 0.4rem 1rem;">Logout</a>
        </div>
    </nav>

    <div class="dashboard-container">
        <div class="sidebar">
            <a href="librarian-dashboard" class="<%= "dashboard".equals(view) ? "active" : "" %>">📊 Overview</a>
            <a href="books?view=books" class="<%= "books".equals(view) || "book_details".equals(view) ? "active" : "" %>">📖 Manage Books</a>
            <a href="student-management" class="<%= "students".equals(view) || "student_details".equals(view) ? "active" : "" %>">👥 Students</a>
            <a href="reserved-books" class="<%= "reserved_books".equals(view) ? "active" : "" %>">🔖 Reserved Books</a>
            <a href="unreserve-requests" class="<%= "unreserve_requests".equals(view) ? "active" : "" %>">↩️ Unreserve Requests</a>
            <a href="librarian-reports" class="<%= "reports".equals(view) ? "active" : "" %>">📈 Reports</a>
            <a href="librarian-dashboard?view=transactions" class="<%= "transactions".equals(view) ? "active" : "" %>">💳 Transactions</a>
            <a href="librarian-dashboard?view=settings" class="<%= "settings".equals(view) ? "active" : "" %>">⚙️ Settings</a>
        </div>
        <div class="main-content">
            <% if(successMsg != null) { %><div class="alert alert-success">✅ <%= successMsg %></div><% } %>
            <% if(errorMsg != null) { %><div class="alert alert-danger">❌ <%= errorMsg %></div><% } %>

            <% if ("dashboard".equals(view)) { 
                Map<String, Object> stats = (Map<String, Object>) request.getAttribute("stats");
                String limit = (String) request.getAttribute("libraryLimit");
            %>
                <h2>Library Overview</h2>
                <div class="stats-grid">
                    <div class="stat-card"><h3>Total Books</h3><p><%= stats != null ? stats.get("totalBooks") : 0 %></p></div>
                    <div class="stat-card"><h3>Currently Borrowed</h3><p><%= stats != null ? stats.get("totalBorrowed") : 0 %></p></div>
                    <div class="stat-card"><h3>Overdue Items</h3><p><%= stats != null ? stats.get("totalOverdue") : 0 %></p></div>
                    <div class="stat-card"><h3>Fines Collected</h3><p>₹<%= stats != null ? stats.get("totalRevenue") : 0 %></p></div>
                </div>
                
                <div style="margin-top: 40px; background: var(--card-bg); padding: 24px; border-radius: 16px; border: 1px solid var(--border-color); box-shadow: var(--shadow-sm);">
                    <h3>System Settings</h3>
                    <form action="librarian-dashboard" method="post" style="display: flex; gap: 10px; align-items: center; margin-top: 15px;">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="update_library_limit">
                        <label for="library_limit" style="font-weight: 500;">Library Book Capacity:</label>
                        <input type="number" id="library_limit" name="library_limit" class="form-control" style="width: 150px;" value="<%= limit %>" required>
                        <button type="submit" class="btn btn-primary" style="width: auto;">Save Limit</button>
                    </form>
                </div>
            <% } else if ("books".equals(view)) { 
                List<Book> books = (List<Book>) request.getAttribute("books");
            %>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <h2>Manage Books</h2>
                    <button class="btn btn-primary" onclick="document.getElementById('addBookForm').style.display='block'; document.getElementById('isbnInput').value = '978' + Math.floor(1000000000 + Math.random() * 9000000000);">+ Add Book</button>
                </div>
                
                <div id="addBookForm" style="display:none; background:var(--card-bg); padding:24px; border-radius:16px; border:1px solid var(--border-color); box-shadow:var(--shadow-md); margin-bottom:24px; backdrop-filter:blur(12px);">
                    <h3 style="margin-bottom: 15px;">Add New Book</h3>
                    <form action="books" method="post" style="display: flex; flex-wrap: wrap; gap: 15px; align-items: center;">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="add">
                        <input type="text" class="form-control" name="title" placeholder="Title" required style="flex: 1; min-width: 200px;">
                        <input type="text" class="form-control" name="author" placeholder="Author" required style="flex: 1; min-width: 200px;">
                        <input type="text" class="form-control" id="isbnInput" name="isbn" placeholder="ISBN" required style="flex: 1; min-width: 150px;">
                        <input type="text" class="form-control" name="category" placeholder="Category" required style="flex: 1; min-width: 150px;">
                        <input type="number" class="form-control" name="copies" placeholder="Copies" required style="width: 120px;">
                        <input type="url" class="form-control" name="imageUrl" placeholder="Cover Image URL (optional)" style="flex: 1; min-width: 250px;">
                        <div style="width: 100%; display: flex; gap: 10px; margin-top: 10px;">
                            <button type="submit" class="btn btn-primary" style="width: auto;">Save Book</button>
                            <button type="button" class="btn btn-secondary" style="width: auto;" onclick="document.getElementById('addBookForm').style.display='none'">Cancel</button>
                        </div>
                    </form>
                </div>

                <table>
                    <tr><th>ID</th><th>Title</th><th>Author</th><th>ISBN</th><th>Category</th><th>Copies (Avail/Total)</th><th>Actions</th></tr>
                    <% if (books != null) { for (Book b : books) { %>
                    <tr>
                        <td><%= b.getBookId() %></td>
                        <td><%= b.getTitle() %></td>
                        <td><%= b.getAuthor() %></td>
                        <td><%= b.getIsbn() %></td>
                        <td><%= b.getCategory() %></td>
                        <td>
                            <div style="display: flex; gap: 8px;">
                                <a href="books?view=book_details&bookId=<%= b.getBookId() %>" class="btn btn-secondary" style="padding: 5px 10px; font-size: 13px; text-decoration: none; width: auto;">Details</a>
                                <form action="books" method="post" style="margin: 0;" onsubmit="return showCustomConfirm('Are you sure you want to delete this book?', this)">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="book_id" value="<%= b.getBookId() %>">
                                    <button type="submit" style="background:#ef4444; color:white; border:none; padding:5px 10px; border-radius:4px; cursor:pointer;">Delete</button>
                                </form>
                            </div>
                        </td>
                    </tr>
                    <% } } %>
                </table>

            <% } else if ("book_details".equals(view)) { 
                Book book = (Book) request.getAttribute("book");
                List<Map<String, Object>> borrowers = (List<Map<String, Object>>) request.getAttribute("borrowers");
            %>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                        <h2 style="margin: 0; color: var(--text-primary);"><%= book.getTitle() %></h2>
                        <span style="color: var(--text-secondary); font-size: 14px;">By <%= book.getAuthor() %> | ISBN: <%= book.getIsbn() %></span>
                    </div>
                    <a href="books?view=books" class="btn btn-secondary" style="width: auto;">← Back to Books</a>
                </div>
                
                <div class="stats-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
                    <div class="stat-card"><h3>Total Copies</h3><p style="font-size: 24px;"><%= book.getCopiesTotal() %></p></div>
                    <div class="stat-card"><h3>Available Copies</h3><p style="font-size: 24px; color: <%= book.getCopiesAvailable() > 0 ? "var(--secondary-color)" : "var(--danger-color)" %>;"><%= book.getCopiesAvailable() %></p></div>
                    <div class="stat-card"><h3>Currently Checked Out</h3><p style="font-size: 24px;"><%= book.getCopiesTotal() - book.getCopiesAvailable() %></p></div>
                </div>

                <h3 style="margin-top: 30px;">Active Borrowers & Reservations</h3>
                <table>
                    <tr><th>Roll No</th><th>Student Name</th><th>Contact Info</th><th>Issue Date</th><th>Due Date</th></tr>
                    <% if (borrowers != null && !borrowers.isEmpty()) { 
                        for (Map<String, Object> map : borrowers) { 
                    %>
                    <tr>
                        <td style="font-weight: 600;"><%= map.get("roll_no") %></td>
                        <td><%= map.get("login_name") %></td>
                        <td style="font-size: 13px;">
                            <div>📧 <%= map.get("email") %></div>
                            <div>📱 <%= map.get("mobile") %></div>
                        </td>
                        <td><%= map.get("issue_date") %></td>
                        <td><%= map.get("due_date") %></td>
                    </tr>
                    <%      } 
                       } else { %>
                    <tr><td colspan="5" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No students are currently holding this book.</td></tr>
                    <% } %>
                </table>



            <% } else if ("students".equals(view)) { 
                List<User> students = (List<User>) request.getAttribute("students");
                List<User> pending = (List<User>) request.getAttribute("pendingStudents");
            %>
                <h2>Student Management</h2>
                
                <% if (pending != null && !pending.isEmpty()) { %>
                <h3>Pending Registrations</h3>
                <table style="margin-bottom:30px; border:2px solid #f59e0b;">
                    <tr style="background:#f59e0b22;"><th>Roll No</th><th>Name</th><th>Email</th><th>Mobile</th><th>Actions</th></tr>
                    <% for (User u : pending) { %>
                    <tr>
                        <td><%= u.getRollNo() %></td>
                        <td><%= u.getLoginName() %></td>
                        <td><%= u.getEmail() %></td>
                        <td><%= u.getMobile() %></td>
                        <td>
                            <form action="student-management" method="post" style="display:inline;">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="login_name" value="<%= u.getLoginName() %>">
                                <button type="submit" name="action" value="approve" class="btn btn-primary" style="padding:4px 8px; font-size:12px;">Approve</button>
                                <button type="submit" name="action" value="reject" class="btn btn-secondary" style="padding:4px 8px; font-size:12px; background:#ef4444; border-color:#ef4444;">Reject</button>
                            </form>
                        </td>
                    </tr>
                    <% } %>
                </table>
                <% } %>

                <div style="background: var(--surface-color); padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 1px solid var(--border-color);">
                    <form action="student-management" method="get" style="display: flex; gap: 10px; align-items: center;">
                        <input type="text" name="searchRollNo" placeholder="Search student by Roll Number..." class="form-control" style="max-width: 300px; padding: 10px; border-radius: 8px; border: 1px solid var(--border-color); flex: 1;" required>
                        <button type="submit" class="btn btn-primary" style="padding: 10px 20px; width: auto;">🔍 Search</button>
                    </form>
                </div>

                <h3>All Students</h3>
                <table>
                    <tr><th>Roll No</th><th>Username</th><th>Email</th><th>Mobile</th></tr>
                    <% if (students != null) { for (User u : students) { %>
                    <tr>
                        <td><%= u.getRollNo() %></td>
                        <td><%= u.getLoginName() %></td>
                        <td><%= u.getEmail() %></td>
                        <td><%= u.getMobile() %></td>
                    </tr>
                    <% } } %>
                </table>

            <% } else if ("student_details".equals(view)) { 
                User student = (User) request.getAttribute("searchResultUser");
                List<Borrow> activeBorrows = (List<Borrow>) request.getAttribute("activeBorrows");
                Double totalFine = (Double) request.getAttribute("totalFine");
            %>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <h2>Student Portfolio: <%= student.getLoginName() %></h2>
                    <a href="student-management" class="btn btn-secondary" style="width: auto;">← Back to Directory</a>
                </div>
                
                <div class="stats-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
                    <div class="stat-card"><h3>Roll Number</h3><p style="font-size: 24px;"><%= student.getRollNo() %></p></div>
                    <div class="stat-card"><h3>Contact Mail</h3><p style="font-size: 16px;"><%= student.getEmail() %></p></div>
                    <div class="stat-card"><h3 style="color: var(--danger-color);">Pending Fines</h3><p style="font-size: 24px; color: var(--danger-color);">₹<%= totalFine != null ? totalFine : 0 %></p></div>
                </div>

                <h3 style="margin-top: 30px;">Borrowing History & Dues</h3>
                <table>
                    <tr><th>Book Title</th><th>Issue Date</th><th>Due Date</th><th>Return Date</th><th>Fine</th><th>Action</th></tr>
                    <% if (activeBorrows != null && !activeBorrows.isEmpty()) { 
                        for (Borrow b : activeBorrows) { 
                            boolean returned = b.getReturnDate() != null;
                    %>
                    <tr>
                        <td><%= b.getBookTitle() %></td>
                        <td><%= b.getIssueDate() %></td>
                        <td><%= b.getDueDate() %></td>
                        <td><%= returned ? b.getReturnDate() : "Pending" %></td>
                        <td>
                            <% if (b.getFineAmount() > 0) { %>
                                <%= b.isFinePaid() ? "Paid" : "₹" + b.getFineAmount() + " (Due)" %>
                            <% } else { out.print("-"); } %>
                        </td>
                        <td>
                            <div style="display: flex; gap: 8px;">
                                <% if (!returned) { %>
                                    <form action="managePayments" method="post" style="margin: 0;">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="approve_student_return">
                                        <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                                        <input type="hidden" name="studentId" value="<%= student.getRollNo() %>">
                                        <input type="hidden" name="searchRollNo" value="<%= student.getRollNo() %>">
                                        <button type="submit" class="btn btn-primary" style="padding: 6px 12px; font-size: 13px; width: auto; background: var(--secondary-color); border: none;">Return</button>
                                    </form>
                                    <% if (b.isUnreserveRequested()) { %>
                                    <form action="student-management" method="post" style="margin: 0;">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="approve_student_unreserve">
                                        <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                                        <input type="hidden" name="searchRollNo" value="<%= student.getRollNo() %>">
                                        <button type="submit" class="btn btn-primary" style="padding: 6px 12px; font-size: 13px; width: auto; background: rgba(245, 158, 11, 0.9); border: none; color: white;">Approve Unreserve</button>
                                    </form>
                                    <% } %>
                                <% } else { %>
                                    <span class="badge badge-approved">Returned</span>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                    <%      } 
                       } else { %>
                    <tr><td colspan="6" style="text-align: center; color: var(--text-secondary);">No borrow history found for this student.</td></tr>
                    <% } %>
                </table>

            <% } else if ("reports".equals(view)) { 
                List<Map<String, Object>> popularBooks = (List<Map<String, Object>>) request.getAttribute("popularBooks");
            %>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <h2>Library Reports</h2>
                    <a href="librarian-reports?export=csv" class="btn btn-primary">⬇️ Export CSV</a>
                </div>
                
                <h3>Most Popular Books</h3>
                <table>
                    <tr><th>Title</th><th>Author</th><th>Borrow Count</th></tr>
                    <% if (popularBooks != null) { for (Map<String, Object> map : popularBooks) { %>
                    <tr>
                        <td><%= map.get("title") %></td>
                        <td><%= map.get("author") %></td>
                        <td><%= map.get("borrowCount") %></td>
                    </tr>
                    <% } } %>
                </table>
                
            <% } else if ("reserved_books".equals(view)) { 
                List<Borrow> reservedBooks = (List<Borrow>) request.getAttribute("reservedBooks");
            %>
                <h2>Reserved Books</h2>
                <table>
                    <tr><th>Roll No</th><th>Student Name</th><th>Book Title</th><th>Reservation Expiry</th><th>Action</th></tr>
                    <% if (reservedBooks != null && !reservedBooks.isEmpty()) { 
                        for (Borrow b : reservedBooks) { %>
                    <tr>
                        <td><%= b.getUserRollNo() %></td>
                        <td><%= b.getUserId() %></td>
                        <td><%= b.getBookTitle() %></td>
                        <td>
                            <%
                                java.sql.Timestamp expiry = b.getReservationExpiry();
                                if (expiry != null && expiry.before(new java.sql.Timestamp(System.currentTimeMillis()))) {
                            %>
                                <span style="color: var(--danger-color); font-weight: bold;">Expired</span>
                            <% } else if (expiry != null) { %>
                                <%= new java.text.SimpleDateFormat("MMM dd, hh:mm a").format(expiry) %>
                            <% } else { %>
                                -
                            <% } %>
                        </td>
                        <td>
                            <form action="reserved-books" method="post" style="margin: 0;">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="mark_borrowed">
                                <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                                <button type="submit" class="btn btn-primary" style="padding: 6px 12px; font-size: 13px; width: auto; background: var(--secondary-color); border: none;" onclick="return showCustomConfirm('Crosscheck: Is the student ID <%= b.getUserId() %> physically present? Give them the book and mark as Borrowed.', this.form)">Mark as Borrowed</button>
                            </form>
                        </td>
                    </tr>
                    <% } } else { %>
                    <tr><td colspan="5" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No reserved books pending pickup.</td></tr>
                    <% } %>
                </table>

            <% } else if ("unreserve_requests".equals(view)) { 
                List<Borrow> unreserveRequests = (List<Borrow>) request.getAttribute("unreserveRequests");
            %>
                <h2>Unreserve Requests</h2>
                <table>
                    <tr><th>Roll No</th><th>Student Name</th><th>Book Title</th><th>Reason</th><th>Actions</th></tr>
                    <% if (unreserveRequests != null && !unreserveRequests.isEmpty()) { 
                        for (Borrow b : unreserveRequests) { %>
                    <tr>
                        <td><%= b.getUserRollNo() %></td>
                        <td><%= b.getUserId() %></td>
                        <td><%= b.getBookTitle() %></td>
                        <td><%= utils.Sanitize.html(b.getUnreserveReason()) %></td>
                        <td>
                            <form action="unreserve-requests" method="post" style="margin: 0;">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="approve_unreserve">
                                <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                                <button type="submit" class="btn btn-primary" style="padding: 6px 12px; font-size: 13px; width: auto; background: var(--secondary-color); border: none;">Approve</button>
                            </form>
                        </td>
                    </tr>
                    <% } } else { %>
                    <tr><td colspan="5" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No unreserve requests pending.</td></tr>
                    <% } %>
                </table>
            <% } else if ("transactions".equals(view)) { 
                List<Borrow> transactions = (List<Borrow>) request.getAttribute("transactions");
            %>
                <h2>Transaction History & Pending Approvals</h2>
                <table>
                    <tr><th>Borrow ID</th><th>Student</th><th>Book</th><th>Fine Amount</th><th>Payment Method</th><th>Status/Action</th></tr>
                    <% if (transactions != null && !transactions.isEmpty()) { 
                        boolean hasTransactions = false;
                        for (Borrow b : transactions) { 
                            if (b.getFineAmount() > 0) {
                                hasTransactions = true;
                                boolean isCashPending = "CASH".equals(b.getPaymentMethod()) && !b.isFinePaid();
                                boolean isPaid = b.isFinePaid();
                    %>
                    <tr>
                        <td><%= b.getBorrowId() %></td>
                        <td><%= b.getUserId() %></td>
                        <td><%= b.getBookTitle() %></td>
                        <td>₹<%= b.getFineAmount() %></td>
                        <td><%= b.getPaymentMethod() != null ? b.getPaymentMethod() : "-" %></td>
                        <td>
                            <% if (isPaid) { %>
                                <span class="badge badge-approved">Completed</span>
                            <% } else if (isCashPending) { %>
                                <form action="managePayments" method="post" style="margin: 0;">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="approve_cash">
                                    <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                                    <input type="hidden" name="studentId" value="<%= b.getUserId() %>">
                                    <button type="submit" class="btn btn-primary" style="padding: 6px 12px; font-size: 13px; width: auto; background: var(--secondary-color); border: none;">Verify & Approve</button>
                                </form>
                            <% } else { %>
                                <span style="color: var(--text-secondary); font-size: 0.9rem;">Pending Payment</span>
                            <% } %>
                        </td>
                    </tr>
                    <%          }
                            }
                            if (!hasTransactions) { %>
                                <tr><td colspan="6" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No fine transactions found.</td></tr>
                    <%      }
                       } else { %>
                        <tr><td colspan="6" style="text-align: center; color: var(--text-secondary); padding: 2rem;">No fine transactions found.</td></tr>
                    <% } %>
                </table>

            <% } else if ("settings".equals(view)) { 
                String libUpiId = (String) request.getAttribute("libUpiId");
                String libUpiName = (String) request.getAttribute("libUpiName");
            %>
                <h2>Payment Settings</h2>
                <div style="background: var(--card-bg); padding: 24px; border-radius: 16px; border: 1px solid var(--border-color); box-shadow: var(--shadow-sm); max-width: 500px; backdrop-filter: blur(12px);">
                    <form action="managePayments" method="post">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="action" value="update_upi">
                        
                        <div style="margin-bottom: 15px;">
                            <label style="display: block; margin-bottom: 8px; font-weight: 500;">Library UPI ID (For PhonePe/GPay)</label>
                            <input type="text" name="upi_id" class="form-control" value="<%= libUpiId != null ? libUpiId : "" %>" placeholder="e.g. library@ybl" required>
                        </div>
                        
                        <div style="margin-bottom: 15px;">
                            <label style="display: block; margin-bottom: 8px; font-weight: 500;">Library UPI Name (Merchant Name)</label>
                            <input type="text" name="upi_name" class="form-control" value="<%= libUpiName != null ? libUpiName : "" %>" placeholder="e.g. Central Library" required>
                        </div>

                        <div style="margin-bottom: 25px;">
                            <label style="display: block; margin-bottom: 8px; font-weight: 500; color: var(--danger-color);">Confirm Password to Update</label>
                            <input type="password" name="password" class="form-control" placeholder="Enter your librarian password" required>
                        </div>
                        
                        <button type="submit" class="btn btn-primary">Save Settings</button>
                    </form>
                </div>
            <% } %>

        </div>
    </div>
    <!-- Custom Modal for Confirms -->
    <div id="customConfirmModal" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10000; align-items: center; justify-content: center; backdrop-filter: blur(4px);">
        <div style="background: var(--card-bg); padding: 2rem; border-radius: 16px; max-width: 400px; width: 90%; box-shadow: 0 20px 40px rgba(0,0,0,0.2); text-align: center;">
            <div style="font-size: 3rem; margin-bottom: 1rem;">🤔</div>
            <h3 style="margin: 0 0 1rem 0; color: var(--text-primary); font-size: 1.25rem;">Are you sure?</h3>
            <p id="customConfirmMessage" style="color: var(--text-secondary); margin-bottom: 2rem; line-height: 1.5;"></p>
            <div style="display: flex; gap: 1rem; justify-content: center;">
                <button id="customConfirmCancel" class="btn btn-secondary" style="width: auto; padding: 0.75rem 1.5rem;">Cancel</button>
                <button id="customConfirmOk" class="btn btn-primary" style="width: auto; padding: 0.75rem 1.5rem;">Yes, Continue</button>
            </div>
        </div>
    </div>

    <script>
        let formToSubmit = null;
        function showCustomConfirm(message, formElement) {
            document.getElementById('customConfirmMessage').innerText = message;
            document.getElementById('customConfirmModal').style.display = 'flex';
            formToSubmit = formElement;
            return false;
        }

        document.getElementById('customConfirmCancel').addEventListener('click', function() {
            document.getElementById('customConfirmModal').style.display = 'none';
            formToSubmit = null;
        });

        document.getElementById('customConfirmOk').addEventListener('click', function() {
            document.getElementById('customConfirmModal').style.display = 'none';
            if (formToSubmit) {
                formToSubmit.submit();
            }
        });
    </script>
</body>
</html>
