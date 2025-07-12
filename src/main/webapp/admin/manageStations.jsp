<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Station" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Stations | TrackEase Admin</title>
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
        
        .station-card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
            transition: all 0.3s ease;
        }
        
        .station-card:hover {
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
    </style>
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="admin-header">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-md-8">
                    <h1>Manage Stations</h1>
                    <p class="mb-0">Add, edit or remove station information</p>
                </div>
                <div class="col-md-4 text-end">
                    <button class="btn btn-light" data-bs-toggle="modal" data-bs-target="#addStationModal">
                        <i class="fas fa-plus me-2"></i>Add Station
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
                    <% if (request.getParameter("stationId") == null) { %>
                        $('#addStationModal').modal('show');
                    <% } else { %>
                        $('#editStationModal').modal('show');
                    <% } %>
                });
            </script>
        <% } %>
        
        <div class="station-card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead>
                            <tr>
                                <th>Station ID</th>
                                <th>Station Code</th>
                                <th>Station Name</th>
                                <th>Location</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% 
                                List<Station> stations = (List<Station>) request.getAttribute("stations");
                                if (stations != null && !stations.isEmpty()) {
                                    for (Station station : stations) {
                            %>
                            <tr>
                                <td><%= station.getStationId() %></td>
                                <td><span class="badge badge-station"><%= station.getStationCode() %></span></td>
                                <td><%= station.getStationName() %></td>
                                <td><%= station.getCity() %>, <%= station.getState() %></td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-warning action-btn" 
                                        onclick="showEditModal(
                                            '<%= station.getStationId() %>',
                                            '<%= station.getStationCode() %>',
                                            '<%= station.getStationName() %>',
                                            '<%= station.getCity() %>',
                                            '<%= station.getState() %>'
                                        )">
                                        <i class="fas fa-edit me-1"></i>Edit
                                    </button>
                                    <form action="station" method="post" style="display: inline;">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="stationId" value="<%= station.getStationId() %>">
                                        <button type="submit" class="btn btn-sm btn-danger action-btn" 
                                            onclick="return confirm('Are you sure you want to delete this station?')">
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
                                    <i class="fas fa-map-marker-alt fa-2x mb-3 text-muted"></i>
                                    <h5 class="text-muted">No stations found</h5>
                                    <button class="btn btn-primary mt-2" data-bs-toggle="modal" data-bs-target="#addStationModal">
                                        <i class="fas fa-plus me-2"></i>Add Your First Station
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
    
    <!-- Add Station Modal -->
    <div class="modal fade" id="addStationModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title"><i class="fas fa-map-marker-alt me-2"></i>Add New Station</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="station" method="post">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="stationCode" class="form-label">Station Code</label>
                            <input type="text" class="form-control" id="stationCode" name="stationCode" required maxlength="10">
                        </div>
                        <div class="mb-3">
                            <label for="stationName" class="form-label">Station Name</label>
                            <input type="text" class="form-control" id="stationName" name="stationName" required>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="city" class="form-label">City</label>
                                <input type="text" class="form-control" id="city" name="city" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="state" class="form-label">State</label>
                                <input type="text" class="form-control" id="state" name="state" required>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="submit" class="btn btn-primary">Add Station</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    
    <!-- Edit Station Modal -->
    <div class="modal fade" id="editStationModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title"><i class="fas fa-edit me-2"></i>Edit Station</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form action="station" method="post">
                    <input type="hidden" name="action" value="edit">
                    <input type="hidden" name="stationId" value="${param.stationId}">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Station Code</label>
                            <input type="text" class="form-control" name="stationCode" 
                                   value="${empty param.stationCode ? stationCode : param.stationCode}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Station Name</label>
                            <input type="text" class="form-control" name="stationName" 
                                   value="${empty param.stationName ? stationName : param.stationName}" required>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label">City</label>
                                <input type="text" class="form-control" name="city" 
                                       value="${empty param.city ? city : param.city}" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label">State</label>
                                <input type="text" class="form-control" name="state" 
                                       value="${empty param.state ? state : param.state}" required>
                            </div>
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
        function showEditModal(id, code, name, city, state) {
            $('input[name="stationId"]').val(id);
            $('input[name="stationCode"]').val(code);
            $('input[name="stationName"]').val(name);
            $('input[name="city"]').val(city);
            $('input[name="state"]').val(state);
            new bootstrap.Modal(document.getElementById('editStationModal')).show();
        }
    </script>
</body>
</html>