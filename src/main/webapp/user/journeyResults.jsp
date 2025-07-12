<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.model.Journey, com.trainticket.model.Station, java.util.List" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Available Journeys</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.8.1/font/bootstrap-icons.css">
    <style>
        .search-summary {
            background-color: #f8f9fa;
            border-radius: 10px;
            padding: 20px;
            margin-bottom: 30px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        .journey-card {
            border: 1px solid #dee2e6;
            border-radius: 10px;
            margin-bottom: 20px;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .journey-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 5px 15px rgba(0,0,0,0.1);
        }
        .journey-header {
            background-color: #f1f8ff;
            border-bottom: 1px solid #dee2e6;
            padding: 15px;
            border-radius: 10px 10px 0 0;
        }
        .train-number {
            font-weight: 600;
            color: #0d6efd;
        }
        .time-display {
            font-size: 1.1rem;
            font-weight: 500;
        }
        .station-code {
            font-size: 0.9rem;
            color: #6c757d;
        }
        .duration-badge {
            background-color: #e9ecef;
            color: #495057;
            font-weight: 500;
        }
        .fare-amount {
            font-size: 1.2rem;
            font-weight: 600;
            color: #198754;
        }
        .seats-available {
            font-size: 0.9rem;
        }
        .seats-badge {
            background-color: #d1e7dd;
            color: #0f5132;
        }
        .no-results {
            padding: 50px 20px;
            text-align: center;
            border-radius: 10px;
        }
        .train-icon {
            color: #0d6efd;
            margin-right: 5px;
        }
        .action-buttons {
            display: flex;
            flex-direction: column;
            justify-content: center;
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="container mt-4 mb-5">
        <div class="search-summary">
            <h3><i class="bi bi-search"></i> Search Results</h3>
            <div class="row mt-3">
                <div class="col-md-4">
                    <p class="mb-1"><strong>From:</strong></p>
                    <h5>
                        <span class="badge bg-primary">
                            <%= ((Station)request.getAttribute("departureStation")).getStationCode() %>
                        </span>
                        <%= ((Station)request.getAttribute("departureStation")).getStationName() %>
                    </h5>
                </div>
                <div class="col-md-4">
                    <p class="mb-1"><strong>To:</strong></p>
                    <h5>
                        <span class="badge bg-primary">
                            <%= ((Station)request.getAttribute("arrivalStation")).getStationCode() %>
                        </span>
                        <%= ((Station)request.getAttribute("arrivalStation")).getStationName() %>
                    </h5>
                </div>
                <div class="col-md-4">
                    <p class="mb-1"><strong>Date:</strong></p>
                    <h5><span class="badge bg-info text-dark"><%= request.getAttribute("travelDate") %></span></h5>
                </div>
            </div>
        </div>
        
        <% 
            List<Journey> journeys = (List<Journey>) request.getAttribute("journeys");
            if (journeys != null && !journeys.isEmpty()) {
        %>
        <div class="row">
            <% for (Journey journey : journeys) { %>
            <div class="col-lg-6">
                <div class="journey-card">
                    <div class="journey-header">
                        <h5>
                            <i class="bi bi-train train-icon"></i>
                            <span class="train-number"><%= journey.getTrain().getTrainNumber() %></span>
                            <%= journey.getTrain().getTrainName() %>
                        </h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-4">
                                <div class="time-display">
                                    <%= journey.getDepartureTime() %>
                                </div>
                                <div class="station-code">
                                    <%= journey.getDepartureStation().getStationCode() %>
                                </div>
                            </div>
                            <div class="col-md-4 text-center">
                                <span class="badge duration-badge">
                                    <% 
                                        long diff = journey.getArrivalTime().getTime() - journey.getDepartureTime().getTime();
                                        long diffHours = diff / (60 * 60 * 1000);
                                        long diffMinutes = (diff / (60 * 1000)) % 60;
                                    %>
                                    <%= diffHours %>h <%= diffMinutes %>m
                                </span>
                                <div class="mt-1">
                                    <small class="text-muted">Duration</small>
                                </div>
                            </div>
                            <div class="col-md-4 text-end">
                                <div class="time-display">
                                    <%= journey.getArrivalTime() %>
                                </div>
                                <div class="station-code">
                                    <%= journey.getArrivalStation().getStationCode() %>
                                </div>
                            </div>
                        </div>
                        
                        <div class="row mt-3 align-items-center">
                            <div class="col-md-4">
                                <div class="fare-amount">₹<%= journey.getFare() %></div>
                                <small class="text-muted">per passenger</small>
                            </div>
                            <div class="col-md-4">
                                <span class="badge seats-badge">
                                    <%= journey.getAvailableSeats() %> seats available
                                </span>
                            </div>
                            <div class="col-md-4 action-buttons">
                                <button class="btn btn-primary btn-sm w-100" data-bs-toggle="modal" 
                                        data-bs-target="#bookTicketModal<%= journey.getJourneyId() %>">
                                    <i class="bi bi-ticket-perforated"></i> Book Now
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Book Ticket Modal for each journey -->
            <div class="modal fade" id="bookTicketModal<%= journey.getJourneyId() %>" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header bg-primary text-white">
                            <h5 class="modal-title">
                                <i class="bi bi-ticket-perforated"></i> Confirm Booking
                            </h5>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <form action="${pageContext.request.contextPath}/user/bookTicket" method="post">
                            <div class="modal-body">
                                <input type="hidden" name="journeyId" value="<%= journey.getJourneyId() %>">
                                <div class="mb-3">
                                    <h6>Journey Details</h6>
                                    <ul class="list-group list-group-flush mb-3">
                                        <li class="list-group-item">
                                            <strong>Train:</strong> 
                                            <%= journey.getTrain().getTrainNumber() %> - <%= journey.getTrain().getTrainName() %>
                                        </li>
                                        <li class="list-group-item">
                                            <strong>From:</strong> 
                                            <%= journey.getDepartureStation().getStationCode() %> at <%= journey.getDepartureTime() %>
                                        </li>
                                        <li class="list-group-item">
                                            <strong>To:</strong> 
                                            <%= journey.getArrivalStation().getStationCode() %> at <%= journey.getArrivalTime() %>
                                        </li>
                                        <li class="list-group-item">
                                            <strong>Fare:</strong> ₹<%= journey.getFare() %>
                                        </li>
                                    </ul>
                                </div>
                                <div class="mb-3">
                                    <label for="seatNumber<%= journey.getJourneyId() %>" class="form-label">
                                        <i class="bi bi-123"></i> Seat Number
                                    </label>
                                    <input type="text" class="form-control" id="seatNumber<%= journey.getJourneyId() %>" 
                                           name="seatNumber" required placeholder="Enter seat number">
                                </div>
                                <div class="mb-3">
                                    <label for="coachNumber<%= journey.getJourneyId() %>" class="form-label">
                                        <i class="bi bi-train-front"></i> Coach Number
                                    </label>
                                    <input type="text" class="form-control" id="coachNumber<%= journey.getJourneyId() %>" 
                                           name="coachNumber" required placeholder="Enter coach number">
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">
                                    <i class="bi bi-x-circle"></i> Cancel
                                </button>
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-check-circle"></i> Confirm Booking
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            <% } %>
        </div>
        <% } else { %>
        <div class="alert alert-info no-results">
            <h4><i class="bi bi-info-circle-fill"></i> No Journeys Found</h4>
            <p class="mt-3">We couldn't find any journeys matching your search criteria.</p>
            <a href="${pageContext.request.contextPath}/user/bookTicket.jsp" class="btn btn-primary mt-3">
                <i class="bi bi-arrow-left"></i> Modify Search
            </a>
        </div>
        <% } %>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>