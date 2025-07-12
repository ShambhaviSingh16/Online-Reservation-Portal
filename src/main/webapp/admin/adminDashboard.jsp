<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.model.User" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | RailExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        :root {
            --primary-color: #4361ee;
            --secondary-color: #3f37c9;
            --accent-color: #4895ef;
            --dark-color: #212529;
            --light-bg: #f8f9fa;
        }
        
        body {
            background-color: var(--light-bg);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        .navbar {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color)) !important;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }
        
        .navbar-brand {
            font-weight: 600;
        }
        
        .card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            transition: all 0.3s ease;
            height: 100%;
        }
        
        .card:hover {
            box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
            transform: translateY(-2px);
        }
        
        .card-title {
            color: var(--primary-color);
            font-weight: 600;
            border-bottom: 2px solid var(--light-bg);
            padding-bottom: 0.75rem;
            margin-bottom: 1rem;
        }
        
        .quick-action-btn {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 1.5rem 0.5rem;
            text-align: center;
            border-radius: 8px;
            transition: all 0.2s ease;
            height: 100%;
        }
        
        .quick-action-btn:hover {
            background-color: rgba(67, 97, 238, 0.1);
            transform: translateY(-3px);
        }
        
        .quick-action-btn i {
            font-size: 1.75rem;
            margin-bottom: 0.75rem;
            color: var(--primary-color);
        }
        
        .profile-icon {
            width: 80px;
            height: 80px;
            background-color: var(--primary-color);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2rem;
            margin: 0 auto 1rem;
        }
        
        .stat-card {
            border-left: 4px solid var(--primary-color);
        }
        
        .stat-number {
            font-size: 1.75rem;
            font-weight: 700;
            color: var(--primary-color);
        }
        
        @media (max-width: 768px) {
            .quick-action-btn {
                padding: 1rem 0.5rem;
            }
            
            .quick-action-btn i {
                font-size: 1.5rem;
                margin-bottom: 0.5rem;
            }
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    <!-- <nav class="navbar navbar-expand-lg navbar-dark">
        <div class="container">
            <a class="navbar-brand" href="#">
                <i class="fas fa-train me-2"></i>RailExpress Admin
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link active" href="adminDashboard.jsp">
                            <i class="fas fa-tachometer-alt me-1"></i>Dashboard
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="tickets">
                            <i class="fas fa-ticket-alt me-1"></i>Tickets
                        </a>
                    </li>
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" id="manageDropdown" role="button" data-bs-toggle="dropdown">
                            <i class="fas fa-cog me-1"></i>Manage
                        </a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="train"><i class="fas fa-train me-2"></i>Trains</a></li>
                            <li><a class="dropdown-item" href="station"><i class="fas fa-map-marker-alt me-2"></i>Stations</a></li>
                            <li><a class="dropdown-item" href="journey"><i class="fas fa-route me-2"></i>Journeys</a></li>
                        </ul>
                    </li>
                </ul>
                <ul class="navbar-nav">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown">
                            <i class="fas fa-user-circle me-1"></i>
                            <%= user.getFullName() != null ? user.getFullName() : "Admin" %>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end">
                            <li><a class="dropdown-item" href="#"><i class="fas fa-user me-2"></i>Profile</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/logout"><i class="fas fa-sign-out-alt me-2"></i>Logout</a></li>
                        </ul>
                    </li>
                </ul>
            </div>
        </div>
    </nav>
    -->

    <div class="container py-4">
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card">
                    <div class="card-body text-center">
                        <div class="profile-icon">
                            <i class="fas fa-user-shield"></i>
                        </div>
                        <h5 class="card-title">Admin Profile</h5>
                        <%
                            User admin = (User) session.getAttribute("user");
                            if (admin != null) {
                        %>
                        <p class="card-text"><strong>Name:</strong> <%= admin.getFullName() != null ? admin.getFullName() : "Admin" %></p>
                        <p class="card-text"><strong>Email:</strong> <%= admin.getEmail() != null ? admin.getEmail() : "N/A" %></p>
                        <p class="card-text"><strong>Role:</strong> <span class="badge bg-primary"><%= admin.getRole() != null ? admin.getRole() : "ADMIN" %></span></p>
                        <% } %>
                    </div>
                </div>
            </div>
            <div class="col-md-8">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Quick Actions</h5>
                        <div class="row g-3">
                            <div class="col-md-3 col-6">
                                <a href="train" class="quick-action-btn">
                                    <i class="fas fa-train"></i>
                                    <span>Manage Trains</span>
                                </a>
                            </div>
                            <div class="col-md-3 col-6">
                                <a href="station" class="quick-action-btn">
                                    <i class="fas fa-map-marker-alt"></i>
                                    <span>Manage Stations</span>
                                </a>
                            </div>
                            <div class="col-md-3 col-6">
                                <a href="journey" class="quick-action-btn">
                                    <i class="fas fa-route"></i>
                                    <span>Manage Journeys</span>
                                </a>
                            </div>
                            <div class="col-md-3 col-6">
                                <a href="tickets" class="quick-action-btn">
                                    <i class="fas fa-ticket-alt"></i>
                                    <span>View Tickets</span>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="row">
            <!-- Stats Cards - You can add dynamic content here later -->
            <div class="col-md-3 mb-4">
                <div class="card stat-card">
                    <div class="card-body">
                        <h6 class="text-muted">Total Trains</h6>
                        <div class="stat-number">25</div>
                        <small class="text-muted">+2 from last month</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-4">
                <div class="card stat-card">
                    <div class="card-body">
                        <h6 class="text-muted">Active Journeys</h6>
                        <div class="stat-number">48</div>
                        <small class="text-muted">+5 from last week</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-4">
                <div class="card stat-card">
                    <div class="card-body">
                        <h6 class="text-muted">Today's Tickets</h6>
                        <div class="stat-number">124</div>
                        <small class="text-muted">+12% from yesterday</small>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-4">
                <div class="card stat-card">
                    <div class="card-body">
                        <h6 class="text-muted">Revenue</h6>
                        <div class="stat-number">₹1,24,500</div>
                        <small class="text-muted">Monthly total</small>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script>
        // You can add dynamic functionality here later
        $(document).ready(function() {
            // Add any dashboard-specific JavaScript here
        });
    </script>
</body>
</html>