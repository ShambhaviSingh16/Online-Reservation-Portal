<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Add Station</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <jsp:include page="../navbar.jsp" />
    
    <div class="container mt-4">
        <div class="card">
            <div class="card-header">
                <h4>Add New Station</h4>
            </div>
            <div class="card-body">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger">${error}</div>
                <% } %>
                
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert alert-success">${success}</div>
                <% } %>
                
                <form action="${pageContext.request.contextPath}/admin/station" method="post">
                    <div class="mb-3">
                        <label for="stationCode" class="form-label">Station Code</label>
                        <input type="text" class="form-control" id="stationCode" name="stationCode" required maxlength="10">
                    </div>
                    <div class="mb-3">
                        <label for="stationName" class="form-label">Station Name</label>
                        <input type="text" class="form-control" id="stationName" name="stationName" required>
                    </div>
                    <div class="mb-3">
                        <label for="city" class="form-label">City</label>
                        <input type="text" class="form-control" id="city" name="city" required>
                    </div>
                    <div class="mb-3">
                        <label for="state" class="form-label">State</label>
                        <input type="text" class="form-control" id="state" name="state" required>
                    </div>
                    <button type="submit" class="btn btn-primary">Add Station</button>
                    <a href="${pageContext.request.contextPath}/admin/manageStations.jsp" class="btn btn-secondary">Cancel</a>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>