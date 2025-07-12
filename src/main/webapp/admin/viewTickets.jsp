<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Ticket, java.util.Collections, java.util.Comparator" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>View Tickets | TrackEase Admin</title>
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
        
        .admin-header {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
            color: white;
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
            background-color: #28a745;
        }
        
        .badge-cancelled {
            background-color: #dc3545;
        }
        
        .badge-pending {
            background-color: #ffc107;
            color: #212529;
        }
        
        .search-box {
            max-width: 400px;
        }
        
        @media (max-width: 768px) {
            .table-responsive {
                border-radius: 8px;
            }
            
            .table th, .table td {
                padding: 0.5rem;
            }
            
            .search-box {
                max-width: 100%;
            }
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="admin-header">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h2> Ticket Management</h2>
                    <p class="mb-0">View and manage all booked tickets</p>
                </div>
                <div class="col-md-4">
                    <div class="input-group search-box">
                        <input type="text" class="form-control" placeholder="Search tickets...">
                        <button class="btn btn-light" type="button">
                            <i class="fas fa-search"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="container mb-5">
        <div class="card-shadow">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead>
                        <tr>
                            <th>Ticket ID</th>
                            <th>Passenger</th>
                            <th>Train</th>
                            <th>Journey</th>
                            <th>Seat</th>
                            <th>Booking Date</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% 
                            List<Ticket> tickets = (List<Ticket>) request.getAttribute("tickets");
                            if (tickets != null && !tickets.isEmpty()) {
                                // Sort tickets by booking date in ASCENDING order (oldest first)
                                Collections.sort(tickets, new Comparator<Ticket>() {
                                    public int compare(Ticket t1, Ticket t2) {
                                        return t1.getBookingDate().compareTo(t2.getBookingDate());
                                    }
                                });
                                
                                
                                for (Ticket ticket : tickets) { 
                                    // Safely handle null user
                                    String userName = (ticket.getUser() != null && ticket.getUser().getFullName() != null) 
                                        ? ticket.getUser().getFullName() : "Guest User";
                                    
                        %>
                        <tr>
                            <td class="fw-bold"><%= ticket.getTicketId() %></td>
                            <td>
                                <span class="fw-bold"><%= userName %></span><br>
                                
                            </td>
                            <td>
                                <span class="fw-bold"><%= ticket.getJourney().getTrain().getTrainNumber() %></span><br>
                                <small class="text-muted"><%= ticket.getJourney().getTrain().getTrainName() %></small>
                            </td>
                            <td>
                                <span class="fw-bold">
                                    <%= ticket.getJourney() != null ? 
                                    ticket.getJourney().getDepartureStation().getStationCode() : "N/A" %> → 
                                    <%= ticket.getJourney() != null ? 
                                    ticket.getJourney().getArrivalStation().getStationCode() : "N/A" %>
                                </span><br>
                                <small class="text-muted">
                                    <%= ticket.getJourney() != null ? ticket.getJourney().getDepartureTime() : "" %>
                                </small>
                            </td>
                            <td>
                                <span class="badge bg-primary status-badge">
                                    <%= ticket.getSeatNumber() %>-<%= ticket.getCoachNumber() %>
                                </span>
                            </td>
                            <td><%= ticket.getBookingDate() %></td>
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
                        </tr>
                        <% 
                                }
                            } else { 
                        %>
                        <tr>
                            <td colspan="7" class="text-center py-4">
                                <i class="fas fa-info-circle fa-2x text-muted mb-3"></i>
                                <h5 class="text-muted">No tickets found</h5>
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>