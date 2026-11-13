<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.trainticket.model.Train" %>
<%@ page import="com.trainticket.model.Station" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="../WEB-INF/ui/head.jsp" />
    <meta charset="UTF-8">
    <title>Add Journey</title>
    
    
</head>
<body class="admin-page">
    <jsp:include page="../navbar.jsp" /><span id="main-content" tabindex="-1"></span>
    
    <div class="container mt-4">
        <div class="card">
            <div class="card-header">
                <h4>Add New Journey</h4>
            </div>
            <div class="card-body">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger">${esc:html(error)}</div>
                <% } %>
                
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert alert-success">${esc:html(success)}</div>
                <% } %>
                
                <form id="journeyForm" action="${esc:html(pageContext.request.contextPath)}/admin/journey" method="post">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                    <div class="mb-3">
                        <label for="trainId" class="form-label">Train</label>
                        <select class="form-select" id="trainId" name="trainId" required>
                            <option value="">Select Train</option>
                            <% 
                                List<Train> trains = (List<Train>) request.getAttribute("trains");
                                if (trains != null) {
                                    for (Train train : trains) {
                            %>
                            <option value="<%= com.trainticket.util.Html.escape(train.getTrainId()) %>"><%= com.trainticket.util.Html.escape(train.getTrainNumber()) %> - <%= com.trainticket.util.Html.escape(train.getTrainName()) %></option>
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
                                <option value="<%= com.trainticket.util.Html.escape(station.getStationId()) %>"><%= com.trainticket.util.Html.escape(station.getStationCode()) %> - <%= com.trainticket.util.Html.escape(station.getStationName()) %></option>
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
                                <option value="<%= com.trainticket.util.Html.escape(station.getStationId()) %>"><%= com.trainticket.util.Html.escape(station.getStationCode()) %> - <%= com.trainticket.util.Html.escape(station.getStationName()) %></option>
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
                            <input type="datetime-local" class="form-control" id="departureTime" name="departureTime" required>
                            <div id="departureTimeError" class="error-message"></div>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label for="arrivalTime" class="form-label">Arrival Time</label>
                            <input type="datetime-local" class="form-control" id="arrivalTime" name="arrivalTime" required>
                            <div id="arrivalTimeError" class="error-message"></div>
                        </div>
                    </div>
                    
                    <div class="mb-3">
                        <label for="fare" class="form-label">Fare (₹)</label>
                        <input type="number" step="0.01" min="0" class="form-control" id="fare" name="fare" required>
                    </div>
                    
                    <div class="mb-3">
                        <label for="availableSeats" class="form-label">Available Seats</label>
                        <input type="number" min="1" class="form-control" id="availableSeats" name="availableSeats" required>
                    </div>
                    
                    <button type="submit" class="btn btn-primary">Add Journey</button>
                    <a href="${esc:html(pageContext.request.contextPath)}/admin/manageJourneys.jsp" class="btn btn-secondary">Cancel</a>
                </form>
            </div>
        </div>
    </div>

    
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Set minimum datetime to current time
            const now = new Date();
            const timezoneOffset = now.getTimezoneOffset() * 60000;
            const localISOTime = (new Date(now - timezoneOffset)).toISOString().slice(0, 16);
            
            document.getElementById('departureTime').min = localISOTime;
            document.getElementById('arrivalTime').min = localISOTime;
            
            // Form validation
            document.getElementById('journeyForm').addEventListener('submit', function(e) {
                let isValid = true;
                
                // Clear previous errors
                document.getElementById('departureTimeError').textContent = '';
                document.getElementById('arrivalTimeError').textContent = '';
                
                // Validate departure and arrival times
                const departureTime = document.getElementById('departureTime').value;
                const arrivalTime = document.getElementById('arrivalTime').value;
                
                if (!departureTime) {
                    document.getElementById('departureTimeError').textContent = 'Please select departure time';
                    isValid = false;
                }
                
                if (!arrivalTime) {
                    document.getElementById('arrivalTimeError').textContent = 'Please select arrival time';
                    isValid = false;
                }
                
                if (departureTime && arrivalTime) {
                    if (new Date(arrivalTime) <= new Date(departureTime)) {
                        document.getElementById('arrivalTimeError').textContent = 'Arrival time must be after departure time';
                        isValid = false;
                    }
                }
                
                if (!isValid) {
                    e.preventDefault();
                }
            });
        });
    </script>
<jsp:include page="../WEB-INF/ui/footer.jsp" /></body>
</html>