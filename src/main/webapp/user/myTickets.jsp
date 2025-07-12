<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Ticket, java.util.Collections, java.util.Comparator" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Tickets | TrackEase</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        :root {
            --primary-color: #4361ee;
            --secondary-color: #3f37c9;
            --accent-color: #4895ef;
            --dark-color: #212529;
            --light-bg: #f8f9fa;
            --success-color: #28a745;
            --danger-color: #dc3545;
            --warning-color: #ffc107;
        }
        
        body {
            background-color: var(--light-bg);
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        .user-header {
            
            padding: 1.5rem 0;
            margin-bottom: 2rem;
            border-radius: 0 0 15px 15px;
        }
        
        .card-shadow {
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            border: none;
            border-radius: 10px;
        }
        
        .table-responsive {
            border-radius: 10px;
            overflow: hidden;
        }
        
        .table thead {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
            color: white;
        }
        
        .table th {
            font-weight: 500;
            padding: 1rem;
        }
        
        .table td {
            vertical-align: middle;
            padding: 0.75rem 1rem;
        }
        
        .status-badge {
            padding: 0.35em 0.65em;
            font-size: 0.75em;
            font-weight: 600;
        }
        
        .badge-confirmed {
            background-color: var(--success-color);
        }
        
        .badge-cancelled {
            background-color: var(--danger-color);
        }
        
        .badge-pending {
            background-color: var(--warning-color);
            color: var(--dark-color);
        }
        
        .route-indicator {
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0.25rem 0;
        }
        
        .route-indicator i {
            color: var(--primary-color);
            margin: 0 0.5rem;
        }
        
        .no-tickets {
            background-color: #f8f9fa;
            border-radius: 10px;
            padding: 3rem;
            text-align: center;
        }
        
        .booking-time {
            font-size: 0.85rem;
            color: #6c757d;
        }
        
        @media (max-width: 768px) {
            .table-responsive {
                border-radius: 8px;
            }
            
            .table th, .table td {
                padding: 0.5rem;
            }
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="user-header">
        <div class="container">
            
        </div>
    </div>

    <div class="container mb-5">
        <% 
            List<Ticket> tickets = (List<Ticket>) request.getAttribute("tickets");
            if (tickets != null && !tickets.isEmpty()) {
                // Sort tickets by booking date in descending order (newest first)
                Collections.sort(tickets, new Comparator<Ticket>() {
                    public int compare(Ticket t1, Ticket t2) {
                        return t2.getBookingDate().compareTo(t1.getBookingDate());
                    }
                });
        %>
        <div class="card-shadow">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead>
                        <tr>
                            <th>Ticket ID</th>
                            <th>Train</th>
                            <th>Route</th>
                            <th>Journey Timings</th>
                            <th>Seat</th>
                            <th>Fare</th>
                            <th>Status</th>
                            <th>Booking Time</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Ticket ticket : tickets) { %>
                        <tr>
                            <td class="fw-bold"><%= ticket.getTicketId() %></td>
                            <td>
                                <span class="badge bg-primary"><%= ticket.getJourney().getTrain().getTrainNumber() %></span><br>
                                <small class="text-muted"><%= ticket.getJourney().getTrain().getTrainName() %></small>
                            </td>
                            <td>
                                <div class="route-indicator">
                                    <span><strong><%= ticket.getJourney().getDepartureStation().getStationCode() %></strong></span>
                                    <i class="fas fa-arrow-right"></i>
                                    <span><strong><%= ticket.getJourney().getArrivalStation().getStationCode() %></strong></span>
                                </div>
                                <small class="text-muted">
                                    <%= ticket.getJourney().getDepartureStation().getStationName() %> → 
                                    <%= ticket.getJourney().getArrivalStation().getStationName() %>
                                </small>
                            </td>
                            <td>
                                <div><strong>Dep:</strong> <%= ticket.getJourney().getDepartureTime() %></div>
                                <div><strong>Arr:</strong> <%= ticket.getJourney().getArrivalTime() %></div>
                            </td>
                            <td>
                                <span class="badge bg-secondary">
                                    <%= ticket.getSeatNumber() %>-<%= ticket.getCoachNumber() %>
                                </span>
                            </td>
                            <td>₹<%= ticket.getJourney().getFare() %></td>
                            <td>
                                <% if ("CONFIRMED".equalsIgnoreCase(ticket.getStatus())) { %>
                                    <span class="badge badge-confirmed status-badge">
                                        <i class="fas fa-check-circle me-1"></i> CONFIRMED
                                    </span>
                                <% } else if ("CANCELLED".equalsIgnoreCase(ticket.getStatus())) { %>
                                    <span class="badge badge-cancelled status-badge">
                                        <i class="fas fa-times-circle me-1"></i> CANCELLED
                                    </span>
                                <% } else { %>
                                    <span class="badge badge-pending status-badge">
                                        <i class="fas fa-clock me-1"></i> <%= ticket.getStatus() != null ? ticket.getStatus() : "PENDING" %>
                                    </span>
                                <% } %>
                            </td>
                            <td class="booking-time">
                                <i class="fas fa-calendar-alt me-1"></i><%= ticket.getBookingDate() %>
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </div>
        <% } else { %>
        <div class="no-tickets">
            <i class="fas fa-ticket-alt fa-4x text-muted mb-4"></i>
            <h4 class="mb-3">No Tickets Found</h4>
            <p class="text-muted mb-4">You haven't booked any tickets yet</p>
            <a href="bookTicket.jsp" class="btn btn-primary px-4">
                <i class="fas fa-plus me-2"></i>Book Your First Ticket
            </a>
        </div>
        <% } %>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>