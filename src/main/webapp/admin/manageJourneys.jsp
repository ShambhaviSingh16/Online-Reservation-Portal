<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.model.Train" %>
<%@ page import="com.trainticket.model.Station" %>
<%@ page import="com.trainticket.model.Journey" %>
<%@ page import="java.util.List" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Journeys | TrackEase Admin</title>
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
        
        .admin-header {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
            color: white;
            padding: 1.5rem 0;
            margin-bottom: 2rem;
            border-radius: 0 0 15px 15px;
        }
        
        .journey-card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            transition: all 0.3s ease;
        }
        
        .journey-card:hover {
            box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
        }
        
        .badge-journey {
            background-color: var(--accent-color);
            color: white;
        }
        .table-responsive {
            border-radius: 10px;
            overflow: hidden;
        }
        
        .table {
            margin-bottom: 0;
        }
        
        .table thead th {
            background-color: var(--primary-color);
            color: white;
            border: none;
        }
        
        .table tbody tr {
            transition: all 0.2s ease;
        }
        
        .table tbody tr:hover {
            background-color: rgba(67, 97, 238, 0.05);
        }
        
        .route-indicator {
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0.5rem 0;
        }
        
        .route-indicator i {
            color: var(--primary-color);
            margin: 0 0.5rem;
        }
        
        .action-btn {
            padding: 0.35rem 0.75rem;
            font-size: 0.85rem;
            border-radius: 6px;
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="admin-header">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h1>Manage Journeys</h1>
                    <p class="mb-0">Add, edit or remove Train journeys</p>
                </div>
                <div class="col-md-4 text-end">
                    <button class="btn btn-light" data-bs-toggle="modal" data-bs-target="#addJourneyModal">
                        <i class="fas fa-plus me-2"></i>Add Journey
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="container mb-5">
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert alert-success alert-dismissible fade show">
                <i class="fas fa-check-circle me-2"></i>${success}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        <% } %>
        
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-danger alert-dismissible fade show">
                <i class="fas fa-exclamation-circle me-2"></i>${error}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
            <script>
                $(document).ready(function() {
                    <% if (request.getParameter("journeyId") == null) { %>
                        $('#addJourneyModal').modal('show');
                    <% } else { %>
                        $('#editJourneyModal').modal('show');
                    <% } %>
                });
            </script>
        <% } %>
        
        <div class="journey-card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead>
                            <tr>
                                <th>Journey ID</th>
                                <th>Train</th>
                                <th>Route</th>
                                <th>Timings</th>
                                <th>Fare</th>
                                <th>Seats</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                                List<Journey> journeys = (List<Journey>) request.getAttribute("journeys");
                                if (journeys != null && !journeys.isEmpty()) {
                                    for (Journey journey : journeys) {
                            %>
                            <tr>
                                <td><%= journey.getJourneyId() %></td>
                                <td>
                                    <span class="badge badge-journey"><%= journey.getTrain().getTrainNumber() %></span>
                                    <%= journey.getTrain().getTrainName() %>
                                </td>
                                <td>
                                    <div><strong><%= journey.getDepartureStation().getStationCode() %></strong> to <strong><%= journey.getArrivalStation().getStationCode() %></strong></div>
                                    <small class="text-muted"><%= journey.getDepartureStation().getStationName() %> → <%= journey.getArrivalStation().getStationName() %></small>
                                </td>
                                <td>
                                    <div><strong>Dep:</strong> <%= journey.getDepartureTime() %></div>
                                    <div><strong>Arr:</strong> <%= journey.getArrivalTime() %></div>
                                </td>
                                <td>₹<%= journey.getFare() %></td>
                                <td><%= journey.getAvailableSeats() %></td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-warning action-btn" data-bs-toggle="modal" data-bs-target="#editJourneyModal"
                                        onclick="setEditFormData(
                                            '<%= journey.getJourneyId() %>',
                                            '<%= journey.getTrain().getTrainId() %>',
                                            '<%= journey.getDepartureStation().getStationId() %>',
                                            '<%= journey.getArrivalStation().getStationId() %>',
                                            '<%= journey.getDepartureTime().toString().replace(" ", "T") %>',
                                            '<%= journey.getArrivalTime().toString().replace(" ", "T") %>',
                                            '<%= journey.getFare() %>',
                                            '<%= journey.getAvailableSeats() %>'
                                        )">
                                        <i class="fas fa-edit me-1"></i>Edit
                                    </button>
                                    <form action="journey" method="post" style="display: inline;">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="journeyId" value="<%= journey.getJourneyId() %>">
                                        <button type="submit" class="btn btn-sm btn-danger action-btn" 
                                            onclick="return confirm('Are you sure you want to delete this journey?')">
                                            <i class="fas fa-trash-alt me-1"></i>Delete
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            <% 
                                    }
                                } else {
                            %>
                            <tr>
                                <td colspan="7" class="text-center py-4">
                                    <i class="fas fa-route fa-2x mb-3 text-muted"></i>
                                    <h5 class="text-muted">No journeys found</h5>
                                    <button class="btn btn-primary mt-2" data-bs-toggle="modal" data-bs-target="#addJourneyModal">
                                        <i class="fas fa-plus me-2"></i>Add Your First Journey
                                    </button>
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Add Journey Modal -->
    <div class="modal fade" id="addJourneyModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title"><i class="fas fa-route me-2"></i>Add New Journey</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="journey" method="post">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="trainId" class="form-label">Train</label>
                            <select class="form-select" id="trainId" name="trainId" required>
                                <option value="">Select Train</option>
                                <% 
                                    List<Train> trains = (List<Train>) request.getAttribute("trains");
                                    if (trains != null) {
                                        for (Train train : trains) {
                                %>
                                <option value="<%= train.getTrainId() %>" <%= request.getParameter("trainId") != null && request.getParameter("trainId").equals(String.valueOf(train.getTrainId())) ? "selected" : "" %>>
                                    <%= train.getTrainNumber() %> - <%= train.getTrainName() %>
                                </option>
                                <% 
                                        }
                                    }
                                %>
                            </select>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="departureStationId" class="form-label">Departure Station</label>
                                <select class="form-select" id="departureStationId" name="departureStationId" required>
                                    <option value="">Select Station</option>
                                    <% 
                                        List<Station> stations = (List<Station>) request.getAttribute("stations");
                                        if (stations != null) {
                                            for (Station station : stations) {
                                    %>
                                    <option value="<%= station.getStationId() %>" <%= request.getParameter("departureStationId") != null && request.getParameter("departureStationId").equals(String.valueOf(station.getStationId())) ? "selected" : "" %>>
                                        <%= station.getStationCode() %> - <%= station.getStationName() %>
                                    </option>
                                    <% 
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="arrivalStationId" class="form-label">Arrival Station</label>
                                <select class="form-select" id="arrivalStationId" name="arrivalStationId" required>
                                    <option value="">Select Station</option>
                                    <% 
                                        if (stations != null) {
                                            for (Station station : stations) {
                                    %>
                                    <option value="<%= station.getStationId() %>" <%= request.getParameter("arrivalStationId") != null && request.getParameter("arrivalStationId").equals(String.valueOf(station.getStationId())) ? "selected" : "" %>>
                                        <%= station.getStationCode() %> - <%= station.getStationName() %>
                                    </option>
                                    <% 
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="departureTime" class="form-label">Departure Time</label>
                                <input type="datetime-local" class="form-control" id="departureTime" name="departureTime" 
                                    value="<%= request.getParameter("departureTime") != null ? request.getParameter("departureTime") : "" %>" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="arrivalTime" class="form-label">Arrival Time</label>
                                <input type="datetime-local" class="form-control" id="arrivalTime" name="arrivalTime" 
                                    value="<%= request.getParameter("arrivalTime") != null ? request.getParameter("arrivalTime") : "" %>" required>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="fare" class="form-label">Fare (₹)</label>
                                <input type="number" step="0.01" class="form-control" id="fare" name="fare" 
                                    value="<%= request.getParameter("fare") != null ? request.getParameter("fare") : "" %>" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="availableSeats" class="form-label">Available Seats</label>
                                <input type="number" class="form-control" id="availableSeats" name="availableSeats" 
                                    value="<%= request.getParameter("availableSeats") != null ? request.getParameter("availableSeats") : "" %>" required>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-primary">Add Journey</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Edit Journey Modal -->
    <div class="modal fade" id="editJourneyModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Edit Journey</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="journey" method="post">
                    <input type="hidden" name="action" value="edit">
                    <input type="hidden" id="editJourneyId" name="journeyId">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="editTrainId" class="form-label">Train</label>
                            <select class="form-select" id="editTrainId" name="trainId" required>
                                <option value="">Select Train</option>
                                <% 
                                    if (trains != null) {
                                        for (Train train : trains) {
                                %>
                                <option value="<%= train.getTrainId() %>"><%= train.getTrainNumber() %> - <%= train.getTrainName() %></option>
                                <% 
                                        }
                                    }
                                %>
                            </select>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="editDepartureStationId" class="form-label">Departure Station</label>
                                <select class="form-select" id="editDepartureStationId" name="departureStationId" required>
                                    <option value="">Select Station</option>
                                    <% 
                                        if (stations != null) {
                                            for (Station station : stations) {
                                    %>
                                    <option value="<%= station.getStationId() %>"><%= station.getStationCode() %> - <%= station.getStationName() %></option>
                                    <% 
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="editArrivalStationId" class="form-label">Arrival Station</label>
                                <select class="form-select" id="editArrivalStationId" name="arrivalStationId" required>
                                    <option value="">Select Station</option>
                                    <% 
                                        if (stations != null) {
                                            for (Station station : stations) {
                                    %>
                                    <option value="<%= station.getStationId() %>"><%= station.getStationCode() %> - <%= station.getStationName() %></option>
                                    <% 
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="editDepartureTime" class="form-label">Departure Time</label>
                                <input type="datetime-local" class="form-control" id="editDepartureTime" name="departureTime" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="editArrivalTime" class="form-label">Arrival Time</label>
                                <input type="datetime-local" class="form-control" id="editArrivalTime" name="arrivalTime" required>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="editFare" class="form-label">Fare (₹)</label>
                                <input type="number" step="0.01" class="form-control" id="editFare" name="fare" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="editAvailableSeats" class="form-label">Available Seats</label>
                                <input type="number" class="form-control" id="editAvailableSeats" name="availableSeats" required>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-primary">Update Journey</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script>
        function setEditFormData(journeyId, trainId, departureStationId, arrivalStationId, departureTime, arrivalTime, fare, availableSeats) {
            document.getElementById('editJourneyId').value = journeyId;
            document.getElementById('editTrainId').value = trainId;
            document.getElementById('editDepartureStationId').value = departureStationId;
            document.getElementById('editArrivalStationId').value = arrivalStationId;
            document.getElementById('editDepartureTime').value = departureTime;
            document.getElementById('editArrivalTime').value = arrivalTime;
            document.getElementById('editFare').value = fare;
            document.getElementById('editAvailableSeats').value = availableSeats;
        }
    </script>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
</body>
</html>