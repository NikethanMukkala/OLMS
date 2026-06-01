<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="models.Book" %>
<%@ page import="models.Borrow" %>
<%@ page import="dao.SettingsDAO" %>
<%@ page import="dao.BorrowDAO" %>
<%
    if (session == null || session.getAttribute("user_id") == null || !"STUDENT".equals(session.getAttribute("role"))) {
        response.sendRedirect("login.jsp?error=Unauthorized access");
        return;
    }
    
    String sessionUserId = (String) session.getAttribute("user_id");
    BorrowDAO _bDao = new BorrowDAO();
    Borrow unratedBorrow = _bDao.getUnratedReturn(sessionUserId);

    SettingsDAO sDao = new SettingsDAO();
    String libUpiId = sDao.getSetting("library_upi_id");
    String libUpiName = sDao.getSetting("library_upi_name");
    
    String viewType = (String) request.getAttribute("viewType");
    if (viewType == null) {
        response.sendRedirect("books"); // redirect to servlet to load dashboard data
        return;
    }

    List<Book> books = (List<Book>) request.getAttribute("books");
    List<Borrow> myBorrows = (List<Borrow>) request.getAttribute("myBorrows");
    Integer totalBooks = (Integer) request.getAttribute("totalBooks");
    Integer availableBooks = (Integer) request.getAttribute("availableBooks");
    Integer reservedByUser = (Integer) request.getAttribute("reservedByUser");
    Integer overdueByUser = (Integer) request.getAttribute("overdueByUser");
    List<String> categories = (List<String>) request.getAttribute("categories");

    String successMsg = (String) session.getAttribute("successMessage");
    String errorMsg = (String) session.getAttribute("errorMessage");
    session.removeAttribute("successMessage");
    session.removeAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OLMS | Student Dashboard</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <script src="js/theme.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .dashboard-container { display: flex; min-height: 100vh; }
        .sidebar { width: 260px; background: var(--surface-color); border-right: 1px solid var(--border-color); padding: 2rem 1.5rem; display: flex; flex-direction: column; position: fixed; left: -260px; top: 0; bottom: 0; z-index: 1000; transition: left 0.3s ease; box-shadow: var(--shadow-lg); }
        .sidebar.open { left: 0; }
        .sidebar-brand { font-size: 1.25rem; font-weight: 800; color: var(--primary-color); margin-bottom: 2.5rem; display: flex; align-items: center; gap: 0.5rem; justify-content: space-between; }
        .close-sidebar { display: none; background: transparent; border: none; font-size: 1.5rem; color: var(--text-primary); cursor: pointer; }
        .sidebar.open .close-sidebar { display: block; }
        .nav-item { padding: 0.85rem 1rem; margin-bottom: 0.5rem; border-radius: 8px; color: var(--text-secondary); text-decoration: none; font-weight: 500; transition: all 0.3s; display: block; }
        .nav-item:hover { background: rgba(99, 102, 241, 0.1); color: var(--primary-color); }
        .nav-item.active { background: var(--primary-color); color: #fff; box-shadow: 0 4px 12px rgba(67, 97, 238, 0.3); }
        .main-content { flex: 1; padding: 2rem 3rem; overflow-y: auto; background: var(--background-color); margin-left: 0; transition: margin-left 0.3s ease; width: 100%; box-sizing: border-box; }
        .hamburger-menu { font-size: 1.5rem; cursor: pointer; background: transparent; border: none; color: var(--text-primary); margin-right: 15px; }
        
        .top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2.5rem; }
        .user-info { font-weight: 600; color: var(--text-primary); }
        
        /* Stats Cards */
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1.5rem; margin-bottom: 3rem; }
        .stat-card { background: var(--surface-color); padding: 1.5rem; border-radius: 16px; box-shadow: var(--shadow-md); border: 1px solid var(--border-color); display: flex; align-items: center; gap: 1rem; transition: transform 0.3s; }
        .stat-card:hover { transform: translateY(-5px); box-shadow: var(--shadow-lg); }
        .stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }
        .stat-icon.primary { background: rgba(67, 97, 238, 0.1); color: var(--primary-color); }
        .stat-icon.success { background: rgba(16, 185, 129, 0.1); color: var(--secondary-color); }
        .stat-icon.warning { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
        .stat-icon.danger { background: rgba(239, 68, 68, 0.1); color: var(--danger-color); }
        .stat-info h3 { font-size: 1.8rem; margin: 0; color: var(--text-primary); }
        .stat-info p { margin: 0; font-size: 0.875rem; color: var(--text-secondary); font-weight: 500; }
        
        /* Search Bar */
        .search-section { background: var(--surface-color); padding: 1.5rem; border-radius: 12px; margin-bottom: 2rem; box-shadow: var(--shadow-sm); border: 1px solid var(--border-color); display: flex; gap: 1rem; align-items: center; }
        .search-input { flex: 1; padding: 0.75rem 1rem; border: 1px solid var(--border-color); border-radius: 8px; background: var(--input-bg); color: var(--text-primary); }
        .search-select { padding: 0.75rem 1rem; border: 1px solid var(--border-color); border-radius: 8px; background: var(--input-bg); color: var(--text-primary); min-width: 150px; }
        
        /* Books Grid */
        .books-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1.5rem; }
        .book-card { background: var(--surface-color); border-radius: 16px; overflow: hidden; box-shadow: var(--shadow-md); border: 1px solid var(--border-color); transition: all 0.3s ease; display: flex; flex-direction: column; }
        .book-card:hover { transform: translateY(-8px); box-shadow: var(--shadow-lg); }
        .book-cover { aspect-ratio: 3/4; width: 100%; background: linear-gradient(135deg, var(--primary-color) 0%, #818cf8 100%); display: flex; align-items: center; justify-content: center; color: white; font-size: 3rem; font-weight: bold; position: relative; }
        .book-badge { position: absolute; top: 12px; right: 12px; padding: 0.25rem 0.75rem; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
        .badge-available { background: var(--secondary-color); color: white; }
        .badge-unavailable { background: var(--danger-color); color: white; }
        .book-details { padding: 1.5rem; flex: 1; display: flex; flex-direction: column; }
        .book-title { font-size: 1.25rem; font-weight: 700; margin-bottom: 0.5rem; color: var(--text-primary); }
        .book-author { color: var(--text-secondary); font-size: 0.9rem; margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem; }
        .book-category { display: inline-block; padding: 0.25rem 0.75rem; background: rgba(67, 97, 238, 0.1); color: var(--primary-color); border-radius: 20px; font-size: 0.8rem; font-weight: 600; margin-bottom: 1.5rem; align-self: flex-start; }
        .book-footer { display: flex; justify-content: space-between; align-items: center; margin-top: auto; padding-top: 1rem; border-top: 1px solid var(--border-color); }
        .copies-info { font-size: 0.85rem; font-weight: 600; color: var(--text-secondary); }
        
        .alert { padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; font-weight: 500; }
        .alert-success { background: rgba(16, 185, 129, 0.1); color: var(--secondary-color); border: 1px solid var(--secondary-color); }
        .alert-error { background: rgba(239, 68, 68, 0.1); color: var(--danger-color); border: 1px solid var(--danger-color); }

        @media (max-width: 768px) {
            .hide-on-mobile { display: none !important; }
            .dashboard-container { flex-direction: column; overflow-x: hidden; }
            .sidebar { width: 100%; max-width: 320px; padding: 1.5rem; border-right: 1px solid var(--border-color); z-index: 1005; left: -320px; }
            .sidebar.open { left: 0; }
            .main-content { padding: 1.5rem 1rem; }
            .search-section { flex-direction: column; background: transparent; box-shadow: none; border: none; padding: 0; margin-bottom: 2rem; }
            .books-grid { grid-template-columns: 1fr; gap: 1.5rem; }
            .stats-grid { grid-template-columns: 1fr 1fr; gap: 1rem; }
            .top-bar { flex-direction: column; align-items: flex-start; gap: 1rem; }
            .top-bar > div { flex-wrap: wrap; }
            .top-bar h2 { font-size: 1.4rem; font-weight: 700; white-space: normal; }
        }
        @media (max-width: 480px) {
            .stats-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <div class="dashboard-container">
        <!-- Sidebar -->
        <aside class="sidebar">
            <div class="sidebar-brand">
                <span>📚 OLMS</span>
                <button class="close-sidebar" id="close-sidebar-btn" title="Close Sidebar">&times;</button>
            </div>
            <nav>
                <a href="books" class="nav-item <%= "books".equals(viewType) ? "active" : "" %>">Dashboard</a>
                <a href="books?view=reservations" class="nav-item <%= "reservations".equals(viewType) ? "active" : "" %>">My Reservations</a>
                <a href="books?view=dues" class="nav-item <%= "dues".equals(viewType) ? "active" : "" %>">Borrowed Books & Dues</a>
                <a href="books?view=history" class="nav-item <%= "history".equals(viewType) ? "active" : "" %>">📜 Transaction History</a>
            </nav>
            <div style="margin-top: auto;">
                <a href="logout" class="nav-item" style="color: var(--danger-color);">🚪 Logout</a>
            </div>
        </aside>

        <!-- Main Content -->
        <main class="main-content">
            <div class="top-bar">
                <div style="display: flex; align-items: center;">
                    <button id="hamburger-btn" class="hamburger-menu" title="Toggle Sidebar">☰</button>
                    <h2 style="margin: 0;">Welcome back, <%= utils.Sanitize.html((String) session.getAttribute("user_id")) %>! 👋</h2>
                </div>
                <div class="user-info">
                    Role: <span style="color: var(--primary-color);"><%= session.getAttribute("role") %></span>
                </div>
            </div>

            <% if(successMsg != null) { %>
                <div class="alert alert-success">✅ <%= utils.Sanitize.html(successMsg) %></div>
            <% } %>
            <% if(errorMsg != null) { %>
                <div class="alert alert-error">❌ <%= utils.Sanitize.html(errorMsg) %></div>
            <% } %>

            <!-- Stats -->
            <div class="stats-grid">
                <div class="stat-card hide-on-mobile">
                    <div class="stat-icon primary">📚</div>
                    <div class="stat-info">
                        <h3><%= totalBooks != null ? totalBooks : 0 %></h3>
                        <p>Total Books</p>
                    </div>
                </div>
                <div class="stat-card hide-on-mobile">
                    <div class="stat-icon success">✨</div>
                    <div class="stat-info">
                        <h3><%= availableBooks != null ? availableBooks : 0 %></h3>
                        <p>Available Now</p>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon warning">📅</div>
                    <div class="stat-info">
                        <h3><%= reservedByUser != null ? reservedByUser : 0 %></h3>
                        <p>Reserved by Me</p>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon danger">⚠️</div>
                    <div class="stat-info">
                        <h3><%= overdueByUser != null ? overdueByUser : 0 %></h3>
                        <p>Overdue Books</p>
                    </div>
                </div>
            </div>

            <% if ("books".equals(viewType)) { %>
            <!-- Search Filter -->
            <form action="books" method="GET" class="search-section">
                <input type="text" name="search" class="search-input" placeholder="Search by title or author..." value="<%= request.getAttribute("searchParam") != null ? utils.Sanitize.html((String) request.getAttribute("searchParam")) : "" %>">
                <select name="category" class="search-select">
                    <option value="">All Categories</option>
                    <% if (categories != null) {
                        String currentCat = (String) request.getAttribute("categoryParam");
                        for (String cat : categories) {
                            String selected = (currentCat != null && currentCat.equals(cat)) ? "selected" : "";
                    %>
                        <option value="<%= cat %>" <%= selected %>><%= cat %></option>
                    <%     }
                       } %>
                </select>
                <button type="submit" class="btn btn-primary" style="width: auto;">🔍 Search</button>
                <a href="books" class="btn btn-secondary" style="width: auto;">Reset</a>
            </form>

            <h3 style="margin-bottom: 1.5rem;">Discover Books</h3>
            
            <!-- Books Grid -->
            <div class="books-grid">
                <% 
                    List<Borrow> activeForUser = (List<Borrow>) request.getAttribute("allMyActiveBorrows");
                    if (books != null && !books.isEmpty()) { 
                    for (Book book : books) {
                        boolean isAvailable = book.getCopiesAvailable() > 0;
                        boolean userHolds = false;
                        if(activeForUser != null) {
                            for(Borrow ab : activeForUser) {
                                if(ab.getBookId() == book.getBookId()) { userHolds = true; break; }
                            }
                        }
                %>
                <div class="book-card">
                    <div class="book-cover" style="<%= (book.getImageUrl() != null && !book.getImageUrl().trim().isEmpty()) ? "background-image: url('" + book.getImageUrl() + "'); background-size: cover; background-position: center;" : "" %>">
                        <%= (book.getImageUrl() == null || book.getImageUrl().trim().isEmpty()) ? book.getTitle().substring(0, 1).toUpperCase() : "" %>
                        <span class="book-badge <%= isAvailable ? "badge-available" : "badge-unavailable" %>">
                            <%= isAvailable ? "Available" : "Checked Out" %>
                        </span>
                    </div>
                    <div class="book-details">
                        <div class="book-title"><%= book.getTitle() %></div>
                        <div class="book-author">✍️ <%= book.getAuthor() %></div>
                        <div class="book-category"><%= book.getCategory() %></div>
                        
                        <div class="book-footer">
                            <span class="copies-info"><%= book.getCopiesAvailable() %> / <%= book.getCopiesTotal() %> left</span>
                            <% if (userHolds) { %>
                                <span style="font-size: 0.85rem; font-weight: bold; color: var(--secondary-color);">✔️ Reserved by You</span>
                            <% } else if (isAvailable) { %>
                                <form action="reserve" method="POST" style="margin: 0;">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="bookId" value="<%= book.getBookId() %>">
                                    <button type="submit" class="btn btn-primary" style="padding: 0.5rem 1rem; width: auto; font-size: 0.85rem;">Reserve</button>
                                </form>
                            <% } else { %>
                                <form action="reserve" method="POST" style="margin: 0;">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="action" value="place_hold">
                                    <input type="hidden" name="bookId" value="<%= book.getBookId() %>">
                                    <button type="submit" class="btn btn-secondary" style="background-color: #6366f1; border-color: #6366f1; color: white; padding: 0.5rem 1rem; width: auto; font-size: 0.85rem;" title="Sign up for the waitlist">Place Hold</button>
                                </form>
                            <% } %>
                        </div>
                    </div>
                </div>
                <%  } 
                   } else { %>
                    <div style="grid-column: 1/-1; text-align: center; padding: 3rem; background: var(--surface-color); border-radius: 12px; border: 1px dashed var(--border-color);">
                        <p style="font-size: 1.2rem; margin-bottom: 0;">No books found matching your criteria.</p>
                        <p style="color: var(--text-secondary);">Try adjusting your search filters.</p>
                    </div>
                <% } %>
            </div>
            <% } else if ("reservations".equals(viewType)) { %>
            <!-- Reservations View -->
            <h3 style="margin-bottom: 1.5rem;">My Reservations</h3>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.5rem;">
                <% if (myBorrows != null && !myBorrows.isEmpty()) { 
                    boolean hasReservations = false;
                    for (Borrow b : myBorrows) {
                        if (!"RESERVED".equals(b.getStatus())) continue;
                        hasReservations = true;
                        long diff = b.getReservationExpiry() != null ? b.getReservationExpiry().getTime() - System.currentTimeMillis() : 0;
                        long hours = diff > 0 ? (long) Math.ceil(diff / (1000.0 * 60 * 60)) : 0;
                        long days = (long) Math.ceil(diff / (1000.0 * 60 * 60 * 24));
                        boolean isOverdue = days < 0;
                %>
                <div class="stat-card" style="flex-direction: column; align-items: flex-start;">
                    <h4 style="margin: 0; font-size: 1.25rem;"><%= b.getBookTitle() %></h4>
                    <p style="margin: 0.5rem 0; color: var(--text-secondary); font-size: 0.9rem;">Author: <%= b.getBookAuthor() %></p>
                    <div style="width: 100%; border-top: 1px dashed var(--border-color); margin: 10px 0;"></div>
                    <p style="margin: 0; font-size: 0.85rem;">Issued: <%= b.getIssueDate() %></p>
                    <p style="margin: 0; font-size: 0.85rem;">Expires: <%= b.getReservationExpiry() != null ? new java.text.SimpleDateFormat("MMM dd, hh:mm a").format(b.getReservationExpiry()) : "-" %></p>
                    <div style="margin-top: 1rem; width: 100%; padding: 0.5rem 1rem; border-radius: 8px; font-weight: bold; text-align: center; background: rgba(16, 185, 129, 0.1); color: var(--secondary-color);">
                        <%= hours > 0 ? hours + " hours to pickup" : "Expired" %>
                    </div>
                    <% if (b.isUnreserveRequested()) { %>
                        <div style="margin-top: 10px; text-align: center; width: 100%;">
                            <span class="badge" style="background: rgba(245, 158, 11, 0.15); color: #d97706; border: 1px solid rgba(245, 158, 11, 0.3);">Unreserve Request Pending</span>
                        </div>
                    <% } else { %>
                        <form action="reserve" method="POST" style="margin-top: 10px; width: 100%; display: flex; flex-direction: column; gap: 8px;">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="unreserve_request">
                            <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                            <input type="text" name="unreserveReason" placeholder="Reason for unreserve" required style="padding: 0.5rem; border-radius: 5px; border: 1px solid var(--border-color);">
                            <button type="submit" class="btn btn-secondary" style="width: 100%; padding: 0.5rem; font-size: 0.85rem;" onclick="return showCustomConfirm('Do you want to request the unreservation of this book? The librarian must approve it.', this.form)">Request Unreserve</button>
                        </form>
                    <% } %>
                </div>
                <%  } 
                   if (!hasReservations) { %>
                    <div style="grid-column: 1/-1; text-align: center; padding: 3rem; background: var(--surface-color); border-radius: 12px; border: 1px dashed var(--border-color);">
                        <p style="font-size: 1.2rem; margin-bottom: 0;">You have no current reservations.</p>
                    </div>
                <% } } else { %>
                    <div style="grid-column: 1/-1; text-align: center; padding: 3rem; background: var(--surface-color); border-radius: 12px; border: 1px dashed var(--border-color);">
                        <p style="font-size: 1.2rem; margin-bottom: 0;">You have no current reservations.</p>
                    </div>
                <% } %>
            </div>
            <% } else if ("dues".equals(viewType)) { %>
            <!-- Dues View -->
            <h3 style="margin-bottom: 1.5rem;">My Borrowed Books &amp; Dues</h3>
            
            <div style="display: flex; gap: 2rem; flex-wrap: wrap; margin-bottom: 2rem;">
                <div style="flex: 1; min-width: 300px;">
                    <canvas id="finePieChart" width="400" height="400" style="max-height: 300px;"></canvas>
                </div>
            </div>

            <div style="overflow-x: auto; background: var(--surface-color); border-radius: 12px; border: 1px solid var(--border-color); box-shadow: var(--shadow-sm);">
                <table style="width: 100%; border-collapse: collapse; min-width: 600px;">
                    <thead style="background: rgba(67, 97, 238, 0.05); border-bottom: 2px solid var(--border-color);">
                        <tr>
                            <th style="padding: 1rem; text-align: left; font-weight: 600;">Title</th>
                            <th style="padding: 1rem; text-align: left; font-weight: 600;">Issue Date</th>
                            <th style="padding: 1rem; text-align: left; font-weight: 600;">Due Date</th>
                            <th style="padding: 1rem; text-align: center; font-weight: 600;">Status</th>
                            <th style="padding: 1rem; text-align: center; font-weight: 600;">Fine</th>
                            <th style="padding: 1rem; text-align: center; font-weight: 600;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                <% if (myBorrows != null && !myBorrows.isEmpty()) { 
                    boolean hasBorrows = false;
                    for (Borrow b : myBorrows) {
                        if ("RESERVED".equals(b.getStatus())) continue;
                        if ("RETURNED".equals(b.getStatus()) && (b.getFineAmount() == 0 || b.isFinePaid())) continue;
                        hasBorrows = true;
                        boolean returned = b.getReturnDate() != null;
                %>
                        <tr style="border-bottom: 1px solid var(--border-color);">
                            <td style="padding: 1rem;"><div style="font-weight: 500;"><%= b.getBookTitle() %></div><div style="font-size: 0.85rem; color: var(--text-secondary);"><%= b.getBookAuthor() %></div></td>
                            <td style="padding: 1rem;"><%= b.getIssueDate() %></td>
                            <td style="padding: 1rem;"><%= b.getDueDate() %></td>
                            <td style="padding: 1rem; text-align: center;">
                                <% if (returned) { %>
                                    <span style="padding: 0.25rem 0.5rem; background: rgba(16, 185, 129, 0.1); color: var(--secondary-color); border-radius: 20px; font-size: 0.85rem;">Returned on <%= b.getReturnDate() %></span>
                                <% } else if (b.getDaysOverdue() > 0) { %>
                                    <span style="padding: 0.25rem 0.5rem; background: rgba(239, 68, 68, 0.1); color: var(--danger-color); border-radius: 20px; font-size: 0.85rem;"><%= b.getDaysOverdue() %> days overdue</span>
                                <% } else { %>
                                    <span style="padding: 0.25rem 0.5rem; background: rgba(245, 158, 11, 0.1); color: #f59e0b; border-radius: 20px; font-size: 0.85rem;">Borrowed</span>
                                <% } %>
                            </td>
                            <td style="padding: 1rem; text-align: center; font-weight: bold; color: <%= b.getFineAmount() > 0 ? "var(--danger-color)" : "inherit" %>;">
                                ₹<%= b.getFineAmount() %>
                                <% if (b.getFineAmount() > 0) { %>
                                    <div style="font-size: 0.75rem; color: var(--text-secondary);"><%= b.isFinePaid() ? "Paid" : "Unpaid" %></div>
                                <% } %>
                            </td>
                            <td style="padding: 1rem; text-align: center;">
                                <% if (!returned) { %>
                                    <form action="returnBook" method="post" style="margin: 0;">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="borrowId" value="<%= b.getBorrowId() %>">
                                        <button type="submit" class="btn btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.85rem; width: auto;" onclick="return showCustomConfirm('Return this book? If overdue, fine will be calculated.', this.form);">Return Book</button>
                                    </form>
                                <% } else if (b.getFineAmount() > 0 && b.isFinePaid()) { %>
                                    <a href="downloadReceipt?borrowId=<%= b.getBorrowId() %>" class="btn btn-secondary" style="padding: 0.4rem 0.8rem; font-size: 0.85rem; width: auto; background: var(--dev-success); border-color: var(--dev-success); color: white; text-decoration: none;">📄 Receipt</a>
                                <% } else if (b.getFineAmount() > 0 && "CASH".equals(b.getPaymentMethod())) { %>
                                    <span style="padding: 0.25rem 0.5rem; background: rgba(245, 158, 11, 0.1); color: #f59e0b; border-radius: 20px; font-size: 0.85rem;">Pending Cash Verification</span>
                                <% } else if (b.getFineAmount() > 0 && !b.isFinePaid()) { %>
                                    <button type="button" class="btn btn-secondary" onclick="openPaymentModal('<%= b.getBorrowId() %>', '<%= b.getFineAmount() %>')" style="padding: 0.4rem 0.8rem; font-size: 0.85rem; width: auto; background: var(--secondary-color); border-color: var(--secondary-color); color: white;">Pay Fine</button>
                                <% } else { %>
                                    <span style="color: var(--text-secondary); font-size: 0.9rem;">-</span>
                                <% } %>
                            </td>
                        </tr>
                <%  } 
                   if (!hasBorrows) { %>
                        <tr>
                            <td colspan="6" style="padding: 3rem; text-align: center; color: var(--text-secondary);">You have no borrowed books.</td>
                        </tr>
                <% } } else { %>
                        <tr>
                            <td colspan="6" style="padding: 3rem; text-align: center; color: var(--text-secondary);">You have no borrowed books.</td>
                        </tr>
                <% } %>
                    </tbody>
                </table>
            </div>

            <!-- Fine Chart Script -->
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    var ctx = document.getElementById('finePieChart');
                    if (!ctx) return;
                    
                    var labels = [];
                    var data = [];
                    var totalFine = 0;
                    
                    <% if (myBorrows != null) {
                        for (Borrow b : myBorrows) {
                            if (b.getFineAmount() > 0 && !b.isFinePaid()) { %>
                                labels.push('<%= b.getBookTitle().replace("'", "\\'") %>');
                                data.push(<%= b.getFineAmount() %>);
                                totalFine += <%= b.getFineAmount() %>;
                            <% }
                        }
                    } %>

                    if (totalFine > 0) {
                        new Chart(ctx, {
                            type: 'pie',
                            data: {
                                labels: labels,
                                datasets: [{
                                    data: data,
                                    backgroundColor: [
                                        '#ef4444', '#f59e0b', '#3b82f6', '#10b981', '#8b5cf6', '#ec4899'
                                    ],
                                    borderWidth: 1
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: {
                                    legend: { position: 'right' },
                                    title: { display: true, text: 'Unpaid Fines Breakdown (Total: ₹' + totalFine + ')' }
                                }
                            }
                        });
                    } else {
                        ctx.style.display = 'none';
                        var container = ctx.parentElement;
                        container.innerHTML = '<div style="background: rgba(16,185,129,0.1); padding: 2rem; border-radius: 12px; text-align: center; color: var(--secondary-color); border: 1px dashed var(--secondary-color);"><strong>All Clear!</strong><br>No outstanding fines.</div>';
                    }
                });
            </script>
            <% } else if ("history".equals(viewType)) { %>
            <!-- History View -->
            <h3 style="margin-bottom: 1.5rem;">Transaction History</h3>
            <div style="overflow-x: auto; background: var(--surface-color); border-radius: 12px; border: 1px solid var(--border-color); box-shadow: var(--shadow-sm);">
                <table style="width: 100%; border-collapse: collapse; min-width: 600px;">
                    <thead style="background: rgba(67, 97, 238, 0.05); border-bottom: 2px solid var(--border-color);">
                        <tr>
                            <th style="padding: 1rem; text-align: left; font-weight: 600;">Title</th>
                            <th style="padding: 1rem; text-align: left; font-weight: 600;">Issue Date</th>
                            <th style="padding: 1rem; text-align: left; font-weight: 600;">Return Date</th>
                            <th style="padding: 1rem; text-align: center; font-weight: 600;">Fine Paid</th>
                        </tr>
                    </thead>
                    <tbody>
                <% if (myBorrows != null && !myBorrows.isEmpty()) { 
                    boolean hasHistory = false;
                    for (Borrow b : myBorrows) {
                        if (!"RETURNED".equals(b.getStatus())) continue;
                        hasHistory = true;
                %>
                        <tr style="border-bottom: 1px solid var(--border-color);">
                            <td style="padding: 1rem;"><div style="font-weight: 500;"><%= b.getBookTitle() %></div><div style="font-size: 0.85rem; color: var(--text-secondary);"><%= b.getBookAuthor() %></div></td>
                            <td style="padding: 1rem;"><%= b.getIssueDate() %></td>
                            <td style="padding: 1rem;"><%= b.getReturnDate() != null ? b.getReturnDate() : "N/A" %></td>
                            <td style="padding: 1rem; text-align: center;">
                                <% if (b.getFineAmount() > 0) { %>
                                    <%= b.isFinePaid() ? "<span style='color: #10b981; font-weight: 500;'>Yes (₹" + b.getFineAmount() + ")</span>" : "<span style='color: #ef4444; font-weight: 500;'>No (₹" + b.getFineAmount() + ")</span>" %>
                                <% } else { %>
                                    <span style="color: var(--text-secondary);">No Fine</span>
                                <% } %>
                            </td>
                        </tr>
                <%  } 
                   if (!hasHistory) { %>
                        <tr><td colspan="4" style="text-align: center; padding: 2rem; color: var(--text-secondary);">No completed transactions found.</td></tr>
                <% } } else { %>
                        <tr><td colspan="4" style="text-align: center; padding: 2rem; color: var(--text-secondary);">No completed transactions found.</td></tr>
                <% } %>
                    </tbody>
                </table>
            </div>
            <% } %>
        </main>
    </div>
    
    <script>
        document.getElementById('hamburger-btn').addEventListener('click', function() {
            document.querySelector('.sidebar').classList.add('open');
        });
        document.getElementById('close-sidebar-btn').addEventListener('click', function() {
            document.querySelector('.sidebar').classList.remove('open');
        });
    </script>

    <!-- LibraryBot Chat Widget -->
    <style>
        #chatbot-btn {
            position: fixed;
            bottom: 80px;
            right: 22px;
            width: 52px;
            height: 52px;
            border-radius: 50%;
            background: linear-gradient(135deg, var(--primary-color), #818cf8);
            color: white;
            font-size: 1.5rem;
            display: flex;
            align-items: center;
            justify-content: center;
            border: none;
            cursor: pointer;
            box-shadow: 0 4px 20px rgba(67,97,238,0.45);
            z-index: 9999;
            transition: transform 0.2s;
        }
        #chatbot-btn:hover { transform: scale(1.1); }
        #chatbot-panel {
            position: fixed;
            bottom: 145px;
            right: 20px;
            width: 360px;
            max-width: calc(100vw - 30px);
            height: 500px;
            max-height: calc(100vh - 160px);
            background: var(--surface-color);
            border: 1px solid var(--border-color);
            border-radius: 18px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.25);
            display: none;
            flex-direction: column;
            z-index: 9998;
            overflow: hidden;
        }
        #chatbot-panel.open { display: flex; animation: slideUp 0.2s ease; }
        @keyframes slideUp { from { opacity:0; transform: translateY(20px);} to { opacity:1; transform: translateY(0);} }
        #chatbot-header {
            padding: 1rem 1.2rem;
            background: linear-gradient(135deg, var(--primary-color), #818cf8);
            color: white;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-radius: 18px 18px 0 0;
        }
        #chatbot-header strong { font-size: 1rem; }
        #chatbot-header span { font-size: 0.78rem; opacity: 0.85; }
        #chatbot-close { background: transparent; border: none; color: white; font-size: 1.4rem; cursor: pointer; line-height: 1; }
        #chatbot-messages {
            flex: 1;
            overflow-y: auto;
            padding: 1rem 1rem 0.5rem;
            display: flex;
            flex-direction: column;
            gap: 0.75rem;
        }
        .chat-msg { max-width: 88%; padding: 0.65rem 0.9rem; border-radius: 14px; font-size: 0.88rem; line-height: 1.5; white-space: pre-wrap; word-break: break-word; }
        .chat-msg.bot { background: var(--background-color); border: 1px solid var(--border-color); align-self: flex-start; border-bottom-left-radius: 4px; }
        .chat-msg.user { background: var(--primary-color); color: white; align-self: flex-end; border-bottom-right-radius: 4px; }
        .chat-msg.typing { color: var(--text-secondary); font-style: italic; }
        #chatbot-input-row {
            display: flex;
            gap: 0.5rem;
            padding: 0.85rem 1rem;
            border-top: 1px solid var(--border-color);
        }
        #chatbot-input {
            flex: 1;
            padding: 0.6rem 0.9rem;
            border: 1px solid var(--border-color);
            border-radius: 10px;
            background: var(--input-bg);
            color: var(--text-primary);
            font-size: 0.9rem;
            outline: none;
        }
        #chatbot-input:focus { border-color: var(--primary-color); }
        #chatbot-send {
            padding: 0.6rem 1rem;
            background: var(--primary-color);
            color: white;
            border: none;
            border-radius: 10px;
            cursor: pointer;
            font-size: 1rem;
            transition: background 0.2s;
        }
        #chatbot-send:hover { background: #3652e8; }
    </style>

    <button id="chatbot-btn" title="LibraryBot – Your assistant">💬</button>

    <div id="chatbot-panel">
        <div id="chatbot-header">
            <div>
                <strong>📚 LibraryBot</strong><br>
                <span>Your personal library assistant</span>
            </div>
            <button id="chatbot-close">✕</button>
        </div>
        <div id="chatbot-messages">
        </div>
        <div id="chatbot-input-row">
            <input id="chatbot-input" type="text" placeholder="Type a command... (e.g. search Java)" autocomplete="off">
            <button id="chatbot-send">➤</button>
        </div>
    </div>

    <script>
        (function() {
            const btn = document.getElementById('chatbot-btn');
            const panel = document.getElementById('chatbot-panel');
            const closeBtn = document.getElementById('chatbot-close');
            const input = document.getElementById('chatbot-input');
            const sendBtn = document.getElementById('chatbot-send');
            const messages = document.getElementById('chatbot-messages');

            btn.addEventListener('click', () => {
                const wasClosed = !panel.classList.contains('open');
                panel.classList.toggle('open');
                if (panel.classList.contains('open')) {
                    input.focus();
                    if (wasClosed && messages.children.length === 0) {
                        const typingEl = appendMsg('Typing...', 'bot typing');
                        fetch('<%= request.getContextPath() %>/chatbot', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ message: "Introduce yourself briefly and ask me what I need." })
                        })
                        .then(r => r.json())
                        .then(data => {
                            typingEl.remove();
                            appendMsg(data.reply || 'Hi!', 'bot');
                        })
                        .catch(() => {
                            typingEl.remove();
                            appendMsg('❌ Could not reach LibraryBot. Please try again.', 'bot');
                        });
                    }
                }
            });
            closeBtn.addEventListener('click', () => panel.classList.remove('open'));

            function appendMsg(text, type) {
                const div = document.createElement('div');
                div.className = 'chat-msg ' + type;
                // Bold **text** support
                div.innerHTML = text
                    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
                    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                    .replace(/\*(.*?)\*/g, '<em>$1</em>')
                    .replace(/`(.*?)`/g, '<code style="background:rgba(99,102,241,0.12);padding:1px 5px;border-radius:4px;">$1</code>');
                messages.appendChild(div);
                messages.scrollTop = messages.scrollHeight;
                return div;
            }

            function sendMessage() {
                const text = input.value.trim();
                if (!text) return;
                appendMsg(text, 'user');
                input.value = '';
                const typingEl = appendMsg('Typing...', 'bot typing');

                fetch('<%= request.getContextPath() %>/chatbot', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: text })
                })
                .then(r => r.json())
                .then(data => {
                    typingEl.remove();
                    appendMsg(data.reply || 'Sorry, something went wrong.', 'bot');
                })
                .catch(() => {
                    typingEl.remove();
                    appendMsg('❌ Could not reach LibraryBot. Please try again.', 'bot');
                });
            }

            sendBtn.addEventListener('click', sendMessage);
            input.addEventListener('keydown', (e) => { if (e.key === 'Enter') sendMessage(); });
        })();
    </script>

    <!-- Payment Modal -->
    <div id="paymentModal" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 10000; align-items: center; justify-content: center;">
        <div style="background: var(--surface-color); padding: 2rem; border-radius: 16px; width: 90%; max-width: 400px; box-shadow: var(--shadow-lg);">
            <h3 style="margin-top: 0; margin-bottom: 1rem;">Pay Fine</h3>
            <p style="margin-bottom: 1.5rem; color: var(--text-secondary);">Amount to Pay: <strong style="color: var(--danger-color);" id="paymentAmountDisplay"></strong></p>
            
            <form action="payFine" method="POST">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="borrowId" id="paymentBorrowId">
                
                <h4 style="margin-bottom: 0.5rem; font-size: 1rem;">Select Payment Method</h4>
                <div style="display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1.5rem;">
                    <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 8px; transition: border-color 0.2s;">
                        <input type="radio" name="paymentMethod" value="phonepe" required>
                        <img src="https://upload.wikimedia.org/wikipedia/commons/7/71/PhonePe_Logo.svg" alt="PhonePe" style="height: 20px;">
                        <span>PhonePe UPI</span>
                    </label>
                    <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 8px; transition: border-color 0.2s;">
                        <input type="radio" name="paymentMethod" value="gpay" required>
                        <img src="https://upload.wikimedia.org/wikipedia/commons/f/f2/Google_Pay_Logo.svg" alt="GPay" style="height: 20px; margin-right: 5px;">
                        <span>Google Pay</span>
                    </label>
                    <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 8px; transition: border-color 0.2s;">
                        <input type="radio" name="paymentMethod" value="cash" required>
                        <span style="font-size: 1.2rem; margin-left: 5px;">💵</span>
                        <span>Pay Cash at Desk</span>
                    </label>
                </div>
                
                <div style="display: flex; gap: 1rem; justify-content: flex-end;">
                    <button type="button" class="btn btn-secondary" onclick="closePaymentModal()" style="width: auto;">Cancel</button>
                    <button type="submit" class="btn btn-primary" style="width: auto; background: var(--secondary-color); border-color: var(--secondary-color);">Confirm Payment</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        window.libraryUpiId = '<%= libUpiId %>';
        window.libraryUpiName = '<%= libUpiName %>'.replace(/ /g, '%20');

        function openPaymentModal(borrowId, amount) {
            document.getElementById('paymentBorrowId').value = borrowId;
            document.getElementById('paymentAmountDisplay').innerText = '₹' + amount;
            window.currentFineAmount = amount;
            document.getElementById('paymentModal').style.display = 'flex';
        }
        function closePaymentModal() {
            document.getElementById('paymentModal').style.display = 'none';
        }
        window.onclick = function(event) {
            var modal = document.getElementById('paymentModal');
            if (event.target == modal) {
                closePaymentModal();
            }
        }

        document.addEventListener('DOMContentLoaded', function() {
            var payForm = document.querySelector('form[action="payFine"]');
            if (payForm) {
                payForm.addEventListener('submit', function(e) {
                    var method = payForm.paymentMethod.value;
                    if (method === 'cash') {
                        return; // proceed with normal form submission
                    }
                    
                    e.preventDefault();
                    var deepLink = '';
                    if (method === 'phonepe') {
                        deepLink = 'phonepe://pay?pa=' + window.libraryUpiId + '&pn=' + window.libraryUpiName + '&am=' + window.currentFineAmount + '&cu=INR';
                    } else if (method === 'gpay') {
                        deepLink = 'tez://upi/pay?pa=' + window.libraryUpiId + '&pn=' + window.libraryUpiName + '&am=' + window.currentFineAmount + '&cu=INR';
                    }
                    
                    if (deepLink) {
                        window.location.href = deepLink;
                        // For simulation of returning from payment gateway without real implementation!
                        setTimeout(function() {
                            payForm.submit();
                        }, 2500);
                    }
                });
            }
        });
    </script>
    
    <% if (unratedBorrow != null) { %>
    <!-- Rating Modal -->
    <div id="ratingModal" style="display: flex; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 10001; align-items: center; justify-content: center; backdrop-filter: blur(5px);">
        <div style="background: var(--surface-color); padding: 2.5rem; border-radius: 16px; width: 90%; max-width: 450px; box-shadow: var(--shadow-lg); text-align: center; border: 1px solid var(--border-color);">
            <h3 style="margin-top: 0; margin-bottom: 0.5rem; font-size: 1.5rem; color: var(--text-primary);">Rate your read! 📚</h3>
            <p style="margin-bottom: 2rem; color: var(--text-secondary); font-size: 1rem;">How did you like <strong><%= unratedBorrow.getBookTitle() %></strong>?</p>
            
            <form action="rate-book" method="POST">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="borrowId" value="<%= unratedBorrow.getBorrowId() %>">
                <input type="hidden" name="bookId" value="<%= unratedBorrow.getBookId() %>">
                <input type="hidden" name="action" id="ratingAction" value="submit_rating">
                <input type="hidden" name="rating" id="ratingValue" value="5">
                
                <div class="stars-container" style="display: flex; justify-content: center; gap: 0.5rem; margin-bottom: 2rem; flex-direction: row-reverse;">
                    <input type="radio" name="star" id="star5" value="5" style="display: none;" checked>
                    <label for="star5" class="star-label" style="font-size: 3rem; cursor: pointer; color: #fbbf24; transition: transform 0.2s; line-height: 1;">★</label>
                    <input type="radio" name="star" id="star4" value="4" style="display: none;">
                    <label for="star4" class="star-label" style="font-size: 3rem; cursor: pointer; color: #d1d5db; transition: transform 0.2s; line-height: 1;">★</label>
                    <input type="radio" name="star" id="star3" value="3" style="display: none;">
                    <label for="star3" class="star-label" style="font-size: 3rem; cursor: pointer; color: #d1d5db; transition: transform 0.2s; line-height: 1;">★</label>
                    <input type="radio" name="star" id="star2" value="2" style="display: none;">
                    <label for="star2" class="star-label" style="font-size: 3rem; cursor: pointer; color: #d1d5db; transition: transform 0.2s; line-height: 1;">★</label>
                    <input type="radio" name="star" id="star1" value="1" style="display: none;">
                    <label for="star1" class="star-label" style="font-size: 3rem; cursor: pointer; color: #d1d5db; transition: transform 0.2s; line-height: 1;">★</label>
                </div>
                
                <style>
                    .star-label:hover,
                    .star-label:hover ~ .star-label { color: #fbbf24 !important; }
                    input[type="radio"]:checked ~ .star-label { color: #fbbf24 !important; }
                    .star-label:hover { transform: scale(1.1); }
                </style>
                <script>
                    const stars = document.querySelectorAll('input[name="star"]');
                    stars.forEach(star => {
                        star.addEventListener('change', (e) => {
                            document.getElementById('ratingValue').value = e.target.value;
                            document.querySelectorAll('.star-label').forEach(l => l.style.color = '#d1d5db');
                            let found = false;
                            document.querySelectorAll('input[name="star"]').forEach(s => {
                                const l = document.querySelector('label[for="'+s.id+'"]');
                                if (!found) l.style.color = '#fbbf24';
                                if (s.checked) found = true;
                            });
                        });
                    });
                </script>
                
                <div style="display: flex; gap: 1rem; justify-content: center;">
                    <button type="button" class="btn btn-secondary" onclick="document.getElementById('ratingAction').value='skip'; this.form.submit();" style="width: auto; padding: 0.75rem 1.75rem;">Skip</button>
                    <button type="submit" class="btn btn-primary" style="width: auto; padding: 0.75rem 1.75rem; background: var(--secondary-color); border-color: var(--secondary-color); font-weight: 600;">Submit Rating</button>
                </div>
            </form>
        </div>
    </div>
    <% } %>

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

