<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.dao.StationDAO, com.trainticket.model.Station, java.util.List" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Book Tickets | TrackEase</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #3a86ff;
            --secondary: #8338ec;
            --accent: #06d6a0;
        }
        body {
            font-family: 'Poppins', sans-serif;
            background: #f8f9fa;
            min-height: 100vh;
        }
        .glass-card {
            background: rgba(255, 255, 255, 0.9);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            box-shadow: 0 8px 32px rgba(31, 38, 135, 0.15);
            border: 1px solid rgba(255, 255, 255, 0.18);
        }
        .form-control, .form-select {
            border-radius: 12px;
            padding: 12px 15px;
            border: 2px solid #e9ecef;
            transition: all 0.3s;
        }
        .form-control:focus, .form-select:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(58, 134, 255, 0.2);
        }
        .input-group-text {
            border-radius: 12px 0 0 12px !important;
            background: white;
            border-right: none;
        }
        .btn-booking {
            background: linear-gradient(135deg, var(--accent) 0%, #2ec4b6 100%);
            border: none;
            border-radius: 12px;
            padding: 14px 35px;
            font-weight: 600;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            transition: all 0.3s;
        }
        .btn-booking:hover {
            transform: translateY(-3px);
            box-shadow: 0 10px 20px rgba(6, 214, 160, 0.3);
        }
        .station-option {
            display: flex;
            justify-content: space-between;
            padding: 8px 12px;
        }
        .station-code {
            font-weight: 600;
            color: var(--primary);
        }
        .station-name {
            color: #6c757d;
            font-size: 0.9em;
        }
        .floating-notification {
            position: fixed;
            bottom: 30px;
            right: 30px;
            z-index: 1000;
            animation: floatUp 0.5s ease-out;
        }
        @keyframes floatUp {
            from { transform: translateY(100px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="container py-5">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <% if (session.getAttribute("error") != null) { %>
                <div class="floating-notification">
                    <div class="alert alert-danger alert-dismissible fade show shadow-lg">
                        <i class="fas fa-exclamation-circle me-2"></i>
                        <%= session.getAttribute("error") %>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                </div>
                <% session.removeAttribute("error"); %>
                <% } %>
                
                <div class="glass-card p-4 p-md-5 mb-5">
                    <div class="text-center mb-5">
                        <h2 class="fw-bold mb-3" style="color: var(--primary);">
                            <i class="fas fa-train me-2"></i> Plan Your Journey
                        </h2>
                        <p class="text-muted">Find the perfect Train for your next adventure</p>
                    </div>
                    
                    <form action="searchJourney" method="post">
                        <div class="row g-4">
                            <!-- Departure Station -->
                            <div class="col-md-6">
                                <label class="form-label fw-500 mb-2">
                                    <i class="fas fa-map-marker-alt me-2 text-primary"></i> From Station
                                </label>
                                <div class="input-group mb-3">
                                    <span class="input-group-text">
                                        <i class="fas fa-map-pin text-primary"></i>
                                    </span>
                                    <select class="form-select" name="departureStation" required>
                                        <option value="">Select departure...</option>
                                        <% for (Station station : new StationDAO().getAllStations()) { %>
                                        <option value="<%= station.getStationId() %>">
                                            <div class="station-option">
                                                <span class="station-code"><%= station.getStationCode() %></span>
                                                <span class="station-name"><%= station.getStationName() %></span>
                                            </div>
                                        </option>
                                        <% } %>
                                    </select>
                                </div>
                            </div>
                            
                            <!-- Arrival Station -->
                            <div class="col-md-6">
                                <label class="form-label fw-500 mb-2">
                                    <i class="fas fa-map-marker-alt me-2 text-danger"></i> To Station
                                </label>
                                <div class="input-group mb-3">
                                    <span class="input-group-text">
                                        <i class="fas fa-flag-checkered text-danger"></i>
                                    </span>
                                    <select class="form-select" name="arrivalStation" required>
                                        <option value="">Select destination...</option>
                                        <% for (Station station : new StationDAO().getAllStations()) { %>
                                        <option value="<%= station.getStationId() %>">
                                            <%= station.getStationCode() %> - <%= station.getStationName() %>
                                        </option>
                                        <% } %>
                                    </select>
                                </div>
                            </div>
                            
                            <!-- Date Picker -->
                            <div class="col-12">
                                <label class="form-label fw-500 mb-2">
                                    <i class="far fa-calendar-alt me-2 text-success"></i> Travel Date
                                </label>
                                <div class="input-group mb-3">
                                    <span class="input-group-text">
                                        <i class="fas fa-calendar-day text-success"></i>
                                    </span>
                                    <input type="date" class="form-control" name="travelDate" required>
                                </div>
                            </div>
                            
                            <!-- Submit Button -->
                            <div class="col-12 text-center mt-4">
                                <button type="submit" class="btn btn-booking btn-lg">
                                    <i class="fas fa-search me-2"></i> Find Trains
                                    <span class="ms-2 badge bg-white text-primary">→</span>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
                
                <!-- Journey Tips 
                <div class="glass-card p-4 mt-4">
                    <div class="d-flex align-items-center mb-3">
                        <i class="fas fa-lightbulb me-3 fs-4 text-warning"></i>
                        <h5 class="mb-0">Pro Tip</h5>
                    </div>
                    <p class="text-muted">
                        Book tickets at least 3 days in advance for best prices. 
                        <a href="#" class="text-primary">View fare calendar</a>
                    </p>
                </div>-->
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Set min date to today
        document.querySelector('input[name="travelDate"]').min = new Date().toISOString().split('T')[0];
        
        // Animate select dropdowns
        document.querySelectorAll('select').forEach(select => {
            select.addEventListener('focus', () => {
                select.parentElement.style.transform = 'translateY(-2px)';
                select.parentElement.style.boxShadow = '0 5px 15px rgba(0,0,0,0.1)';
            });
            select.addEventListener('blur', () => {
                select.parentElement.style.transform = '';
                select.parentElement.style.boxShadow = '';
            });
        });
    </script>
</body>
</html>