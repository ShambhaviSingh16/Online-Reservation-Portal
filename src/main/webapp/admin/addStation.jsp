<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
<jsp:include page="../WEB-INF/ui/head.jsp" />
    <meta charset="UTF-8">
    <title>Add Station</title>
    
</head>
<body class="admin-page">
    <jsp:include page="../navbar.jsp" /><span id="main-content" tabindex="-1"></span>
    
    <div class="container mt-4">
        <div class="card">
            <div class="card-header">
                <h4>Add New Station</h4>
            </div>
            <div class="card-body">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger">${esc:html(error)}</div>
                <% } %>
                
                <% if (request.getAttribute("success") != null) { %>
                    <div class="alert alert-success">${esc:html(success)}</div>
                <% } %>
                
                <form action="${esc:html(pageContext.request.contextPath)}/admin/station" method="post">
                            <input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}">
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
                    <a href="${esc:html(pageContext.request.contextPath)}/admin/manageStations.jsp" class="btn btn-secondary">Cancel</a>
                </form>
            </div>
        </div>
    </div>

    
<jsp:include page="../WEB-INF/ui/footer.jsp" /></body>
</html>