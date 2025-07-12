<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Train" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Trains | TrackEase Admin</title>
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
        
        .card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            transition: all 0.3s ease;
        }
        
        .card:hover {
            box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
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
        
        .badge-station {
            background-color: var(--accent-color);
            color: white;
        }
        
        .action-btn {
            padding: 0.35rem 0.75rem;
            font-size: 0.85rem;
            border-radius: 6px;
        }
        
        .modal-header {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
            color: white;
            border-radius: 10px 10px 0 0 !important;
        }
        
        .modal-title {
            font-weight: 600;
        }
        
        .btn-close-white {
            filter: invert(1);
        }
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="admin-header">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h1>Manage Trains</h1>
                    <p class="mb-0">Add, edit or remove Train information</p>
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
                    <% if (request.getParameter("trainId") == null) { %>
                        $('#addTrainModal').modal('show');
                    <% } else { %>
                        $('#editTrainModal').modal('show');
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
                                <td><%= train.getTrainId() %></td>
                                <td><span class="badge badge-station"><%= train.getTrainNumber() %></span></td>
                                <td><%= train.getTrainName() %></td>
                                <td><%= train.getTotalSeats() %></td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-warning action-btn" 
                                        onclick="showEditModal(
                                            '<%= train.getTrainId() %>',
                                            '<%= train.getTrainNumber() %>',
                                            '<%= train.getTrainName() %>',
                                            '<%= train.getTotalSeats() %>'
                                        )">
                                        <i class="fas fa-edit me-1"></i>Edit
                                    </button>
                                    <form action="train" method="post" style="display: inline;">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="trainId" value="<%= train.getTrainId() %>">
                                        <button type="submit" class="btn btn-sm btn-danger action-btn" 
                                            onclick="return confirm('Are you sure you want to delete this train?')">
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
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="trainNumber" class="form-label">Train Number</label>
                            <input type="text" class="form-control" id="trainNumber" name="trainNumber" 
                                   value="${empty param.trainNumber ? trainNumber : param.trainNumber}" required>
                        </div>
                        <div class="mb-3">
                            <label for="trainName" class="form-label">Train Name</label>
                            <input type="text" class="form-control" id="trainName" name="trainName" 
                                   value="${empty param.trainName ? trainName : param.trainName}" required>
                        </div>
                        <div class="mb-3">
                            <label for="totalSeats" class="form-label">Total Seats</label>
                            <input type="number" class="form-control" id="totalSeats" name="totalSeats" 
                                   value="${empty param.totalSeats ? totalSeats : param.totalSeats}" required>
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
                    <input type="hidden" name="action" value="edit">
                    <input type="hidden" name="trainId" value="${param.trainId}">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Train Number</label>
                            <input type="text" class="form-control" name="trainNumber" 
                                   value="${empty param.trainNumber ? trainNumber : param.trainNumber}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Train Name</label>
                            <input type="text" class="form-control" name="trainName" 
                                   value="${empty param.trainName ? trainName : param.trainName}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Total Seats</label>
                            <input type="number" class="form-control" name="totalSeats" 
                                   value="${empty param.totalSeats ? totalSeats : param.totalSeats}" required>
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

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script>
        function showEditModal(id, number, name, seats) {
            $('input[name="trainId"]').val(id);
            $('input[name="trainNumber"]').val(number);
            $('input[name="trainName"]').val(name);
            $('input[name="totalSeats"]').val(seats);
            new bootstrap.Modal(document.getElementById('editTrainModal')).show();
        }
    </script>
</body>
</html>