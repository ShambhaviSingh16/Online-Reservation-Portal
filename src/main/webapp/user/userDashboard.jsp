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
    <title>User Dashboard | TrackEase</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        :root {
            --primary-color: #4361ee;
            --secondary-color: #3f37c9;
            --accent-color: #4895ef;
            --dark-color: #212529;
            --light-bg: #f8f9fa;
            --success-color: #4cc9f0;
        }
        
        body {
            background-color: var(--light-bg);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        .user-header {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
            color: white;
            padding: 2rem 0;
            margin-bottom: 2rem;
            border-radius: 0 0 20px 20px;
        }
        
        .profile-icon {
        width: 100px;
        height: 100px;
        background: linear-gradient(135deg, var(--accent-color), var(--primary-color));
        color: white;
        font-size: 2.5rem;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 1.5rem; /* Changed from negative margin to normal */
        box-shadow: 0 5px 15px rgba(67, 97, 238, 0.4);
        border: 4px solid white;
        position: relative;
        top: 0; /* Remove any positioning that might clip it */
        transform: none; /* Remove transform that might affect visibility */
    }

    /* Adjust the profile card padding */
    .profile-card .card-body {
        padding-top: 2rem; /* Increased from pt-4 to give more space */
    }

    /* Ensure the profile card has enough space */
       
        .profile-card {
            border: none;
            border-radius: 15px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
            transition: all 0.3s ease;
            overflow: visible;
             padding-top: 1.5rem; /* Add padding to prevent clipping */
        }
        
        .profile-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.12);
        }
        
           
        .action-card {
            border: none;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
            transition: all 0.3s ease;
            height: 100%;
            padding: 1.5rem;
            text-align: center;
            background: white;
        }
        
        .action-card:hover {
            transform: translateY(-8px);
            box-shadow: 0 12px 20px rgba(0, 0, 0, 0.1);
        }
        
        .action-icon {
            font-size: 2.5rem;
            margin-bottom: 1rem;
            color: var(--primary-color);
        }
        
        .upcoming-journeys {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
            padding: 1.5rem;
        }
        
        .journey-item {
            border-left: 4px solid var(--primary-color);
            padding-left: 1rem;
            margin-bottom: 1rem;
            transition: all 0.2s ease;
        }
        
        .journey-item:hover {
            background-color: rgba(67, 97, 238, 0.05);
            transform: translateX(5px);
        }
        
        .footer {
            background: linear-gradient(135deg, var(--dark-color), #2b2d42);
            color: white;
            padding: 3rem 0 1rem;
            margin-top: 3rem;
        }
        
        .footer-link {
            color: #adb5bd;
            text-decoration: none;
            transition: color 0.3s;
        }
        
        .footer-link:hover {
            color: white;
            text-decoration: none;
        }
        
        .social-icon {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: rgba(255, 255, 255, 0.1);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            margin-right: 0.5rem;
            transition: all 0.3s ease;
        }
        
        .social-icon:hover {
            background-color: var(--primary-color);
            transform: translateY(-3px);
        }
        
        @media (max-width: 768px) {
            .profile-icon {
                width: 80px;
                height: 80px;
                font-size: 2rem;
                margin-top: -40px;
            }
            
            .user-header {
                padding: 1.5rem 0;
                border-radius: 0 0 15px 15px;
            }
            
            .action-icon {
                font-size: 2rem;
            }
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="user-header">
        <div class="container text-center">
            <h1 class="fw-bold mb-3">Welcome Back, <%= user.getFullName() %></h1>
            <p class="lead mb-0">Ready for your next adventure?</p>
        </div>
    </div>

    <div class="container mb-5">
        <div class="row g-4">
            <!-- Profile Card -->
            <div class="col-lg-4">
                <div class="profile-card">
                    <div class="card-body text-center pt-4">
                        <div class="profile-icon">
                            <i class="fas fa-user"></i>
                        </div>
                        <h3 class="mb-4">My Profile</h3>
                        <div class="text-start px-3">
                            <div class="mb-3">
                                <small class="text-muted">FULL NAME</small>
                                <p class="mb-0 fw-bold"><%= user.getFullName() %></p>
                            </div>
                            <div class="mb-3">
                                <small class="text-muted">EMAIL ADDRESS</small>
                                <p class="mb-0 fw-bold"><%= user.getEmail() %></p>
                            </div>
                            <div class="mb-3">
                                <small class="text-muted">USERNAME</small>
                                <p class="mb-0 fw-bold"><%= user.getUsername() %></p>
                            </div>
                            <!-- <div class="d-grid mt-4">
                                <a href="editProfile.jsp" class="btn btn-outline-primary">
                                    <i class="fas fa-user-edit me-2"></i>Edit Profile
                                </a>
                            </div>-->
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Main Content -->
            <div class="col-lg-8">
                <!-- Quick Actions -->
                <div class="row g-4 mb-4">
                    <div class="col-md-6">
                        <a href="bookTicket.jsp" class="text-decoration-none">
                            <div class="action-card">
                                <div class="action-icon">
                                    <i class="fas fa-ticket-alt"></i>
                                </div>
                                <h4>Book Ticket</h4>
                                <p class="mb-0 text-muted">Reserve your seat for the next journey</p>
                            </div>
                        </a>
                    </div>
                    <div class="col-md-6">
                        <a href="tickets" class="text-decoration-none">
                            <div class="action-card">
                                <div class="action-icon">
                                    <i class="fas fa-clipboard-list"></i>
                                </div>
                                <h4>My Tickets</h4>
                                <p class="mb-0 text-muted">View and manage your bookings</p>
                            </div>
                        </a>
                    </div>
                    <% if (user.getRole().equals("admin")) { %>
                    <div class="col-md-6">
                        <a href="adminDashboard.jsp" class="text-decoration-none">
                            <div class="action-card">
                                <div class="action-icon">
                                    <i class="fas fa-tachometer-alt"></i>
                                </div>
                                <h4>Admin Panel</h4>
                                <p class="mb-0 text-muted">Manage system operations</p>
                            </div>
                        </a>
                    </div>
                    <% } %>
                    <div class="col-md-6">
                        <a href="${pageContext.request.contextPath}/logout" class="text-decoration-none">
                            <div class="action-card" style="background-color: #FFF0F0;">
                                <div class="action-icon text-danger">
                                    <i class="fas fa-sign-out-alt"></i>
                                </div>
                                <h4 class="text-danger">Logout</h4>
                                <p class="mb-0 text-muted">Secure sign out from system</p>
                            </div>
                        </a>
                    </div>
                </div>
                
                <!-- Upcoming Journeys Section -->
                <div class="upcoming-journeys">
                    <h3 class="mb-4"><i class="fas fa-calendar-alt me-2"></i>Upcoming Journeys</h3>
                    <div class="journey-item">
                        <div class="d-flex justify-content-between">
                            <div>
                                <h5 class="mb-1">Rajdhani Express (12345)</h5>
                                <p class="mb-1 text-muted">New Delhi (NDLS) → Mumbai (CSTM)</p>
                                <small class="text-muted">Departure: May 15, 2023 - 16:30</small>
                            </div>
                            <div class="text-end">
                                <span class="badge bg-success">Confirmed</span>
                                <div class="mt-2">
                                    <a href="#" class="btn btn-sm btn-outline-primary">View Ticket</a>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="journey-item">
                        <div class="d-flex justify-content-between">
                            <div>
                                <h5 class="mb-1">Shatabdi Express (12010)</h5>
                                <p class="mb-1 text-muted">Bangalore (SBC) → Chennai (MAS)</p>
                                <small class="text-muted">Departure: May 20, 2023 - 07:15</small>
                            </div>
                            <div class="text-end">
                                <span class="badge bg-warning text-dark">Pending</span>
                                <div class="mt-2">
                                    <a href="#" class="btn btn-sm btn-outline-secondary">Check Status</a>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="text-center mt-3">
                        <a href="tickets" class="btn btn-link">View All Journeys →</a>
                    </div>
                </div>
            </div>
        </div>
    </div>

   
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // You can add any user dashboard specific JavaScript here
        document.addEventListener('DOMContentLoaded', function() {
            // Example: Add any interactive elements here
        });
    </script>
</body>
</html>