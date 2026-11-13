<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.trainticket.model.User" %>
<% User navUser=(User)session.getAttribute("user"); boolean navAdmin=navUser!=null && "admin".equals(navUser.getRole()); %>
<a class="skip-link" href="#main-content">Skip to content</a>
<header class="site-nav"><nav class="shell nav-inner" aria-label="Main navigation">
<a class="brand" href="${esc:html(pageContext.request.contextPath)}/"><span class="brand-mark" aria-hidden="true"><i class="fas fa-train"></i></span>TrackEase<em>.</em></a>
<button class="nav-toggle" data-nav-toggle aria-controls="main-navigation" aria-expanded="false" aria-label="Toggle navigation"><i class="fas fa-bars" aria-hidden="true"></i></button>
<div class="site-links" id="main-navigation">
<% if(navAdmin) { %>
<a href="${esc:html(pageContext.request.contextPath)}/admin/adminDashboard.jsp">Overview</a><a href="${esc:html(pageContext.request.contextPath)}/admin/train">Trains</a><a href="${esc:html(pageContext.request.contextPath)}/admin/station">Stations</a><a href="${esc:html(pageContext.request.contextPath)}/admin/journey">Journeys</a><a href="${esc:html(pageContext.request.contextPath)}/admin/tickets">Bookings</a>
<% } else if(navUser!=null) { %>
<a href="${esc:html(pageContext.request.contextPath)}/user/userDashboard.jsp">Overview</a><a href="${esc:html(pageContext.request.contextPath)}/user/bookTicket.jsp">Find trains</a><a href="${esc:html(pageContext.request.contextPath)}/user/tickets">My tickets</a>
<% } else { %><a href="${esc:html(pageContext.request.contextPath)}/">Home</a><a href="${esc:html(pageContext.request.contextPath)}/user/bookTicket.jsp">Find trains</a><a href="${esc:html(pageContext.request.contextPath)}/#how-it-works">How it works</a><% } %>
<div class="nav-account"><% if(navUser!=null) { %><div class="nav-person"><strong><%= com.trainticket.util.Html.escape(navUser.getFullName()) %></strong><small><%= com.trainticket.util.Html.escape(navAdmin ? "Administrator" : "Passenger account") %></small></div><form action="${esc:html(pageContext.request.contextPath)}/logout" method="post"><input type="hidden" name="csrfToken" value="${esc:html(sessionScope.csrfToken)}"><button class="button secondary small" type="submit">Logout <i class="fas fa-arrow-right-from-bracket" aria-hidden="true"></i></button></form><% } else { %><a class="text-link" href="${esc:html(pageContext.request.contextPath)}/login.jsp">Login</a><a class="button small" href="${esc:html(pageContext.request.contextPath)}/register.jsp">Create account <span aria-hidden="true">↗</span></a><% } %></div></div></nav></header>