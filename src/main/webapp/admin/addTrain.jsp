<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="../WEB-INF/ui/head.jsp" />
    <meta charset="UTF-8">
    <title>Add Train</title>
    
</head>
<body class="admin-page">
    <jsp:include page="../navbar.jsp" /><span id="main-content" tabindex="-1"></span>
    
    <div class="container mt-4">
        <div class="card">
            <div class="card-header">
                <h4>Add New Train</h4>
            </div>
            <div class="card-body">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger">${esc:html(error)}</div>
                <% } %>
                
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert alert-success">${esc:html(success)}</div>
                <% } %>
                
                <form action="${esc:html(pageContext.request.contextPath)}/admin/train" method="post">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
                    <div class="mb-3">
                        <label for="trainNumber" class="form-label">Train Number</label>
                        <input type="text" class="form-control" id="trainNumber" name="trainNumber" required>
                    </div>
                    <div class="mb-3">
                        <label for="trainName" class="form-label">Train Name</label>
                        <input type="text" class="form-control" id="trainName" name="trainName" required>
                    </div>
                    <div class="mb-3">
                        <label for="totalSeats" class="form-label">Total Seats</label>
                        <input type="number" class="form-control" id="totalSeats" name="totalSeats" required min="1">
                    </div>
                    <button type="submit" class="btn btn-primary">Add Train</button>
                    <a href="${esc:html(pageContext.request.contextPath)}/admin/manageTrains.jsp" class="btn btn-secondary">Cancel</a>
                </form>
            </div>
        </div>
    </div>

    
<jsp:include page="../WEB-INF/ui/footer.jsp" /></body>
</html>