<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.trainticket.model.Station" %>

<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="../WEB-INF/ui/head.jsp" />
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Stations | TrackEase Admin</title>
    
    
    
</head>
<body class="admin-page">
    <jsp:include page="../navbar.jsp" /><span id="main-content" tabindex="-1"></span>
    
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
                    <% if (request.getParameter("stationId") == null) { %>
                        bootstrap.Modal.getOrCreateInstance(document.getElementById('addStationModal')).show();
                    <% } else { %>
                        bootstrap.Modal.getOrCreateInstance(document.getElementById('editStationModal')).show();
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
                                <td><%= com.trainticket.util.Html.escape(station.getStationId()) %></td>
                                <td><span class="badge badge-station"><%= com.trainticket.util.Html.escape(station.getStationCode()) %></span></td>
                                <td><%= com.trainticket.util.Html.escape(station.getStationName()) %></td>
                                <td><%= com.trainticket.util.Html.escape(station.getCity()) %>, <%= com.trainticket.util.Html.escape(station.getState()) %></td>
                                <td class="text-end">
                                    <button class="btn btn-sm btn-warning action-btn" 
                                        onclick="showEditModal(
                                            '<%= com.trainticket.util.Html.escape(station.getStationId()) %>',
                                            '<%= com.trainticket.util.Html.escape(station.getStationCode()) %>',
                                            '<%= com.trainticket.util.Html.escape(station.getStationName()) %>',
                                            '<%= com.trainticket.util.Html.escape(station.getCity()) %>',
                                            '<%= com.trainticket.util.Html.escape(station.getState()) %>'
                                        )">
                                        <i class="fas fa-edit me-1"></i>Edit
                                    </button>
                                    <form action="station" method="post" style="display: inline;">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="stationId" value="<%= com.trainticket.util.Html.escape(station.getStationId()) %>">
                                        <button type="submit" class="btn btn-sm btn-danger action-btn" 
                                            onclick="return confirm('Archive this station? Existing tickets will be preserved.')">
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
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
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
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                    <input type="hidden" name="action" value="edit">
                    <input type="hidden" name="stationId" value="${esc:html(param.stationId)}">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Station Code</label>
                            <input type="text" class="form-control" name="stationCode" 
                                   value="${esc:html(empty param.stationCode ? stationCode : param.stationCode)}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Station Name</label>
                            <input type="text" class="form-control" name="stationName" 
                                   value="${esc:html(empty param.stationName ? stationName : param.stationName)}" required>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label">City</label>
                                <input type="text" class="form-control" name="city" 
                                       value="${esc:html(empty param.city ? city : param.city)}" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label">State</label>
                                <input type="text" class="form-control" name="state" 
                                       value="${esc:html(empty param.state ? state : param.state)}" required>
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

    
    <script src="${esc:html(pageContext.request.contextPath)}/assets/vendor/jquery.min.js"></script>
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
<jsp:include page="../WEB-INF/ui/footer.jsp" /></body>
</html>
