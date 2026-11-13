<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Train" %>

<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="../WEB-INF/ui/head.jsp" />
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Trains | TrackEase Admin</title>
    
    
    
</head>
<body class="admin-page">
    <jsp:include page="../navbar.jsp" /><span id="main-content" tabindex="-1"></span>
    
    <div class="admin-header">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h1>Manage Trains</h1>
                    <p class="mb-0">Manage your fleet, seating capacity, and active train records.</p>
                </div>
                <div class="col-md-4 text-end">
                    <button class="btn btn-light" data-bs-toggle="modal" data-bs-target="#addTrainModal">
                        <i class="fas fa-plus me-2"></i>Add Train
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="container mb-5">
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert alert-success alert-dismissible fade show">
                <i class="fas fa-check-circle me-2"></i>${esc:html(success)}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        <% } %>
        
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-danger alert-dismissible fade show">
                <i class="fas fa-exclamation-circle me-2"></i>${esc:html(error)}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    <% if (request.getParameter("trainId") == null) { %>
                        bootstrap.Modal.getOrCreateInstance(document.getElementById('addTrainModal')).show();
                    <% } else { %>
                        bootstrap.Modal.getOrCreateInstance(document.getElementById('editTrainModal')).show();
                    <% } %>
                });
            </script>
        <% } %>
        
        <div class="card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead>
                            <tr>
                                <th>Train ID</th>
                                <th>Train Number</th>
                                <th>Train Name</th>
                                <th>Total Seats</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                                List<Train> trains = (List<Train>) request.getAttribute("trains");
                                if (trains != null && !trains.isEmpty()) {
                                    for (Train train : trains) {
                            %>
                            <tr>
                                <td><%= com.trainticket.util.Html.escape(train.getTrainId()) %></td>
                                <td><span class="badge badge-station"><%= com.trainticket.util.Html.escape(train.getTrainNumber()) %></span></td>
                                <td><%= com.trainticket.util.Html.escape(train.getTrainName()) %></td>
                                <td><%= com.trainticket.util.Html.escape(train.getTotalSeats()) %></td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-warning action-btn" 
                                        onclick="showEditModal(
                                            '<%= com.trainticket.util.Html.escape(train.getTrainId()) %>',
                                            '<%= com.trainticket.util.Html.escape(train.getTrainNumber()) %>',
                                            '<%= com.trainticket.util.Html.escape(train.getTrainName()) %>',
                                            '<%= com.trainticket.util.Html.escape(train.getTotalSeats()) %>'
                                        )">
                                        <i class="fas fa-edit me-1"></i>Edit
                                    </button>
                                    <form action="train" method="post" style="display: inline;">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="trainId" value="<%= com.trainticket.util.Html.escape(train.getTrainId()) %>">
                                        <button type="submit" class="btn btn-sm btn-danger action-btn" 
                                            onclick="return confirm('Archive this train? Existing tickets will be preserved.')">
                                            <i class="fas fa-box-archive me-1"></i>Archive
                                        </button>
                                    </form>
                                </td>
                            </tr>
                            <% 
                                    }
                                } else {
                            %>
                            <tr>
                                <td colspan="5" class="text-center py-4">
                                    <i class="fas fa-train fa-2x mb-3 text-muted"></i>
                                    <h5 class="text-muted">No Trains found</h5>
                                    <button class="btn btn-primary mt-2" data-bs-toggle="modal" data-bs-target="#addTrainModal">
                                        <i class="fas fa-plus me-2"></i>Add Your First Train
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
    
    <!-- Add Train Modal -->
    <div class="modal fade" id="addTrainModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fas fa-train me-2"></i>Add New Train</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="train" method="post">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="trainNumber" class="form-label">Train Number</label>
                            <input type="text" class="form-control" id="trainNumber" name="trainNumber" 
                                   value="${esc:html(empty param.trainNumber ? trainNumber : param.trainNumber)}" required>
                        </div>
                        <div class="mb-3">
                            <label for="trainName" class="form-label">Train Name</label>
                            <input type="text" class="form-control" id="trainName" name="trainName" 
                                   value="${esc:html(empty param.trainName ? trainName : param.trainName)}" required>
                        </div>
                        <div class="mb-3">
                            <label for="totalSeats" class="form-label">Total Seats</label>
                            <input type="number" class="form-control" id="totalSeats" name="totalSeats" 
                                   value="${esc:html(empty param.totalSeats ? totalSeats : param.totalSeats)}" required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-primary">Add Train</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <!-- Edit Train Modal -->
    <div class="modal fade" id="editTrainModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Edit Train</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="train" method="post">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                    <input type="hidden" name="action" value="edit">
                    <input type="hidden" name="trainId" value="${esc:html(param.trainId)}">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Train Number</label>
                            <input type="text" class="form-control" name="trainNumber" 
                                   value="${esc:html(empty param.trainNumber ? trainNumber : param.trainNumber)}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Train Name</label>
                            <input type="text" class="form-control" name="trainName" 
                                   value="${esc:html(empty param.trainName ? trainName : param.trainName)}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Total Seats</label>
                            <input type="number" class="form-control" name="totalSeats" 
                                   value="${esc:html(empty param.totalSeats ? totalSeats : param.totalSeats)}" required>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    
    <script src="${esc:html(pageContext.request.contextPath)}/assets/vendor/jquery.min.js"></script>
    <script>
        function showEditModal(id, number, name, seats) {
            $('input[name="trainId"]').val(id);
            $('input[name="trainNumber"]').val(number);
            $('input[name="trainName"]').val(name);
            $('input[name="totalSeats"]').val(seats);
            new bootstrap.Modal(document.getElementById('editTrainModal')).show();
        }
    </script>
<jsp:include page="../WEB-INF/ui/footer.jsp" /></body>
</html>
