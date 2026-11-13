<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Ticket, java.util.Collections, java.util.Comparator" %>
<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="../WEB-INF/ui/head.jsp" />
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>View Tickets | TrackEase Admin</title>
    
    
    
</head>
<body class="admin-page">
    <jsp:include page="../navbar.jsp" /><span id="main-content" tabindex="-1"></span>
    
    <div class="admin-header">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <span class="eyebrow">TrackEase operations</span>
                    <h1>Booking management</h1>
                    <p class="mb-0">View and manage all booked tickets</p>
                </div>
                <div class="col-md-4">
                    <div class="input-group search-box">
                        <label class="visually-hidden" for="bookingSearch">Search by ticket, passenger, train or status</label>
                        <input type="search" id="bookingSearch" class="form-control" placeholder="Search bookings..." data-booking-search>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="container mb-5">
        <p class="field-help" data-booking-search-count aria-live="polite">Search by ticket, passenger, train or status.</p>
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
                                    String userName = ticket.getPassengerName()!=null ? ticket.getPassengerName() : (ticket.getUser() != null && ticket.getUser().getFullName() != null) 
                                        ? ticket.getUser().getFullName() : "Guest User";
                                    
                        %>
                        <tr data-booking-row>
                            <td class="fw-bold"><%= com.trainticket.util.Html.escape(ticket.getTicketId()) %></td>
                            <td>
                                <span class="fw-bold"><%= com.trainticket.util.Html.escape(userName) %></span><br>
                                
                            </td>
                            <td>
                                <span class="fw-bold"><%= com.trainticket.util.Html.escape(ticket.getJourney().getTrain().getTrainNumber()) %></span><br>
                                <small class="text-muted"><%= com.trainticket.util.Html.escape(ticket.getJourney().getTrain().getTrainName()) %></small>
                            </td>
                            <td>
                                <span class="fw-bold">
                                    <%= com.trainticket.util.Html.escape(ticket.getJourney() != null ? 
                                    ticket.getJourney().getDepartureStation().getStationCode() : "N/A") %> → 
                                    <%= com.trainticket.util.Html.escape(ticket.getJourney() != null ? 
                                    ticket.getJourney().getArrivalStation().getStationCode() : "N/A") %>
                                </span><br>
                                <small class="text-muted">
                                    <%= com.trainticket.util.Html.escape(ticket.getJourney() != null ? ticket.getJourney().getDepartureTime() : "") %>
                                </small>
                            </td>
                            <td>
                                <span class="badge bg-primary status-badge">
                                    <%= com.trainticket.util.Html.escape(ticket.getSeatNumber()) %>-<%= com.trainticket.util.Html.escape(ticket.getCoachNumber()) %>
                                </span>
                            </td>
                            <td><%= com.trainticket.util.Html.escape(ticket.getBookingDate()) %></td>
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
                                        <i class="fas fa-clock me-1"></i> <%= com.trainticket.util.Html.escape(ticket.getStatus() != null ? ticket.getStatus() : "PENDING") %>
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

    
<jsp:include page="../WEB-INF/ui/footer.jsp" /></body>
</html>
