<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.model.User" %>
<style>
    :root {
        --primary-color: #4361ee;
        --secondary-color: #3f37c9;
        --accent-color: #4895ef;
        --dark-color: #212529;
    }
    
    .navbar {
        padding: 0.8rem 1rem;
        box-shadow: 0 2px 15px rgba(0, 0, 0, 0.1);
        background: linear-gradient(135deg, var(--primary-color), var(--secondary-color)) !important;
    }
    
    .navbar-brand {
        font-weight: 700;
        letter-spacing: 0.5px;
        font-size: 1.5rem;
    }
    
    .navbar-brand i {
        color: #fff;
        transition: transform 0.3s ease;
    }
    
    .navbar-brand:hover i {
        transform: scale(1.1);
    }
    
    /* Special hover effect for specific nav items */
    .nav-item-special .nav-link {
        font-weight: 500;
        padding: 0.5rem 1rem !important;
        margin: 0 0.2rem;
        border-radius: 8px;
        transition: all 0.3s ease;
        position: relative;
        overflow: hidden;
    }
    
    .nav-item-special .nav-link::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        width: 0;
        height: 2px;
        background: #fff;
        transform: translateX(-50%);
        transition: width 0.3s ease;
    }
    
    .nav-item-special .nav-link:hover::after {
        width: 70%;
    }
    
    .nav-item-special .nav-link:hover {
        transform: translateY(-2px);
    }
    
    .nav-link {
        font-weight: 500;
        padding: 0.5rem 1rem !important;
        margin: 0 0.2rem;
        border-radius: 8px;
    }
    
    .nav-link.active {
        background-color: rgba(255, 255, 255, 0.25);
        font-weight: 600;
    }
    
    .dropdown-menu {
        border: none;
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
        border-radius: 8px;
        padding: 0;
        margin-top: 0.5rem;
        overflow: hidden; /* This fixes the hover overflow */
    }
    
    .dropdown-item {
        padding: 0.75rem 1.5rem;
        transition: all 0.2s ease;
        margin: 0;
    }
    
    .dropdown-item:hover {
        background-color: var(--accent-color);
        color: white;
        transform: none; /* Removed the translateX to prevent overflow */
    }
    
    
    
    .navbar-toggler {
        border: none;
        padding: 0.5rem;
    }
    
    .navbar-toggler:focus {
        box-shadow: none;
    }
    
    .badge-notification {
        position: absolute;
        top: -5px;
        right: -5px;
        font-size: 0.6rem;
        padding: 0.25em 0.4em;
    }
</style>

<nav class="navbar navbar-expand-lg navbar-dark sticky-top">
    <div class="container">
        <a class="navbar-brand" href="${pageContext.request.contextPath}/userDashboard.jsp">
            <i class="fas fa-train me-2"></i>TrackEase
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <%
                User user = (User) session.getAttribute("user");
                if (user != null) {
                    if (user.getRole().equals("admin")) {
            %>
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" href="adminDashboard.jsp"><i class="fas fa-tachometer-alt me-1"></i> Dashboard</a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" id="adminDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fas fa-cogs me-1"></i> Manage
                    </a>
                    <ul class="dropdown-menu" aria-labelledby="adminDropdown">
                        <li><a class="dropdown-item" href="train"><i class="fas fa-train me-2"></i> Train</a></li>
                        <li><a class="dropdown-item" href="station"><i class="fas fa-map-marker-alt me-2"></i> Stations</a></li>
                        <li><a class="dropdown-item" href="journey"><i class="fas fa-route me-2"></i> Journeys</a></li>
                        <!-- <li><hr class="dropdown-divider"></li>
                        
                        .dropdown-divider {
       							 margin: 0.3rem 0;
   										   }
    <li><a class="dropdown-item" href="tickets"><i class="fas fa-ticket-alt me-2"></i> All Tickets</a></li>-->
                    </ul>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="tickets"><i class="fas fa-chart-bar me-1"></i> All Tickets</a>
                </li>
            </ul>
            <% } else { %>
            <ul class="navbar-nav me-auto">
                <li class="nav-item nav-item-special">
                    <a class="nav-link" href="userDashboard.jsp"><i class="fas fa-home me-1"></i> Home</a>
                </li>
                <li class="nav-item nav-item-special">
                    <a class="nav-link" href="tickets"><i class="fas fa-ticket-alt me-1"></i> My Tickets</a>
                </li>
                <li class="nav-item nav-item-special">
                    <a class="nav-link" href="bookTicket.jsp"><i class="fas fa-plus-circle me-1"></i> Book Ticket</a>
                </li>
                <li class="nav-item">
                    <!-- <a class="nav-link" href="history.jsp"><i class="fas fa-history me-1"></i> Travel History</a>-->
                </li>
            </ul>
            <% } %>
            <ul class="navbar-nav ms-auto">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <div class="me-2 d-none d-sm-inline">
                            <div class="fw-bold"><%= user.getFullName() %></div>
                            <div class="small text-white-50"><%= user.getRole().equals("admin") ? "Administrator" : "Passenger" %></div>
                        </div>
                        <i class="fas fa-user-circle fa-lg"></i>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="userDropdown">
                        <!-- <li><a class="dropdown-item" href="profile.jsp"><i class="fas fa-user me-2"></i> Profile</a></li>
                        <li><a class="dropdown-item" href="settings.jsp"><i class="fas fa-cog me-2"></i> Settings</a></li>
                        <li><hr class="dropdown-divider"></li>-->
                        <li><a class="dropdown-item text-danger" href="${pageContext.request.contextPath}/logout"><i class="fas fa-sign-out-alt me-2"></i> Logout</a></li>
                    </ul>
                </li>
            </ul>
            <% } else { %>
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="login.jsp"><i class="fas fa-sign-in-alt me-1"></i> Login</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link btn btn-outline-light ms-2" href="register.jsp"><i class="fas fa-user-plus me-1"></i> Register</a>
                </li>
            </ul>
            <% } %>
        </div>
    </div>
</nav>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">