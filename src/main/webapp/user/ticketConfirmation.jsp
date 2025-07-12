<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.dao.TicketDAO, com.trainticket.model.Ticket" %>
<%
    Ticket ticket = null;
    String ticketIdParam = request.getParameter("ticketId");
    if (ticketIdParam != null && !ticketIdParam.trim().isEmpty()) {
        try {
            int ticketId = Integer.parseInt(ticketIdParam);
            TicketDAO ticketDAO = new TicketDAO();
            ticket = ticketDAO.getTicketById(ticketId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Ticket Confirmation | TrackEase</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        :root {
            --primary: #3a86ff;
            --secondary: #8338ec;
            --accent: #06d6a0;
        }
        body {
            background-color: #f5f7fa;
        }
        .ticket-card {
            border-radius: 16px;
            border: 1px solid rgba(0,0,0,0.1);
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
            overflow: hidden;
            max-width: 700px;
        }
        .ticket-header {
            background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
            color: white;
            padding: 20px;
            position: relative;
        }
        .ticket-header:after {
            content: "";
            position: absolute;
            bottom: -10px;
            left: 0;
            right: 0;
            height: 20px;
            background: url('data:image/svg+xml;utf8,<svg viewBox="0 0 100 10" xmlns="http://www.w3.org/2000/svg"><path d="M0,0 Q50,10 100,0" fill="white"/></svg>') bottom center repeat-x;
            background-size: 100px 20px;
        }
        .ticket-body {
            padding: 25px;
            background: white;
        }
        .journey-info {
            position: relative;
            padding: 20px 0;
        }
        .journey-route {
            position: relative;
            z-index: 1;
        }
        .journey-route:before {
            content: "";
            position: absolute;
            left: 50%;
            top: 0;
            bottom: 0;
            width: 2px;
            background: linear-gradient(to bottom, var(--primary), var(--secondary));
            transform: translateX(-50%);
            z-index: -1;
        }
        .qr-code {
            width: 150px;
            height: 150px;
            background: #f8f9fa;
            border: 1px dashed #ddd;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto;
        }
        .btn-print {
            background: var(--primary);
            border: none;
            padding: 10px 25px;
            border-radius: 8px;
            font-weight: 500;
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="container py-5">
        <% if (ticket != null) { %>
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="ticket-card mx-auto">
                    <div class="ticket-header text-center">
                        <h3 class="mb-1"><i class="fas fa-check-circle me-2"></i> Booking Confirmed</h3>
                        <p class="mb-0">Your ticket is ready!</p>
                    </div>
                    
                    <div class="ticket-body">
                        <div class="row mb-4">
                            <div class="col-md-6">
                                <h5 class="text-muted mb-3">Ticket Details</h5>
                                <p><strong>Ticket ID:</strong> #<%= ticket.getTicketId() %></p>
                                <p><strong>Booking Date:</strong> <%= ticket.getBookingDate() %></p>
                                <p><strong>Status:</strong> <span class="badge bg-success"><%= ticket.getStatus() %></span></p>
                            </div>
                            <div class="col-md-6 text-end">
                                <div class="qr-code">
                                    <i class="fas fa-qrcode fa-3x text-muted"></i>
                                </div>
                            </div>
                        </div>
                        
                        <div class="journey-info">
                            <div class="row journey-route text-center mb-4">
                                <div class="col-5">
                                    <h4 class="text-primary"><%= ticket.getJourney().getDepartureStation().getStationCode() %></h4>
                                    <p class="text-muted mb-0"><%= ticket.getJourney().getDepartureTime() %></p>
                                </div>
                                <div class="col-2 align-self-center">
                                    <i class="fas fa-arrow-right text-muted"></i>
                                </div>
                                <div class="col-5">
                                    <h4 class="text-primary"><%= ticket.getJourney().getArrivalStation().getStationCode() %></h4>
                                    <p class="text-muted mb-0"><%= ticket.getJourney().getArrivalTime() %></p>
                                </div>
                            </div>
                            
                            <div class="row">
                                <div class="col-md-4">
                                    <h6 class="text-muted">Train</h6>
                                    <p><i class="fas fa-train text-primary me-2"></i> <%= ticket.getJourney().getTrain().getTrainNumber() %></p>
                                </div>
                                <div class="col-md-4">
                                    <h6 class="text-muted">Seat</h6>
                                    <p><i class="fas fa-chair text-primary me-2"></i> <%= ticket.getSeatNumber() %> (<%= ticket.getCoachNumber() %>)</p>
                                </div>
                                <div class="col-md-4">
                                    <h6 class="text-muted">Fare</h6>
                                    <p><i class="fas fa-rupee-sign text-primary me-2"></i> <%= ticket.getJourney().getFare() %></p>
                                </div>
                            </div>
                        </div>
                        
                        <div class="d-flex justify-content-between mt-4">
                            <a href="${pageContext.request.contextPath}/user/bookTicket.jsp" class="btn btn-outline-primary">
                                <i class="fas fa-ticket-alt me-2"></i> Book Another
                            </a>
                            <button onclick="window.print()" class="btn-print btn text-white">
                                <i class="fas fa-print me-2"></i> Print Ticket
                            </button>
                            <a href="${pageContext.request.contextPath}/user/userDashboard.jsp" class="btn btn-outline-secondary">
                                <i class="fas fa-home me-2"></i> Dashboard
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <% } else { %>
        <div class="row justify-content-center">
            <div class="col-lg-6">
                <div class="card border-danger">
                    <div class="card-body text-center py-5">
                        <i class="fas fa-exclamation-triangle fa-4x text-danger mb-4"></i>
                        <h3 class="text-danger">Ticket Not Found</h3>
                        <p class="mb-4">We couldn't retrieve your ticket information. Please contact support.</p>
                        <div class="d-flex justify-content-center gap-3">
                            <a href="${pageContext.request.contextPath}/user/bookTicket.jsp" class="btn btn-primary">
                                <i class="fas fa-search me-2"></i> Search Again
                            </a>
                            <a href="${pageContext.request.contextPath}/user/userDashboard.jsp" class="btn btn-secondary">
                                <i class="fas fa-home me-2"></i> Return Home
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <% } %>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>