<%@ taglib prefix="esc" uri="/WEB-INF/functions.tld" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.trainticket.dao.TicketDAO,com.trainticket.model.Ticket,com.trainticket.model.User,java.util.List,java.util.UUID" %>
<%
User owner=(User)session.getAttribute("user");
if(owner==null){response.sendRedirect(request.getContextPath()+"/login.jsp");return;}
String bookingId;
try { bookingId=UUID.fromString(request.getParameter("bookingId")).toString(); }
catch(IllegalArgumentException | NullPointerException e){response.setStatus(400);out.print("Invalid booking reference.");return;}
List<Ticket> passengers=new TicketDAO().getBookingGroup(owner.getUserId(),bookingId);
if(passengers.isEmpty()){response.setStatus(404);out.print("Booking not found.");return;}
Ticket first=passengers.get(0);
long confirmed=passengers.stream().filter(t -> "CONFIRMED".equals(t.getStatus())).count();
long waiting=passengers.stream().filter(t -> "WAITLISTED".equals(t.getStatus())).count();
long rac=passengers.stream().filter(t -> "RAC".equals(t.getStatus())).count();
long cancelled=passengers.size()-confirmed-waiting-rac;
java.math.BigDecimal total=passengers.stream().filter(t -> !"CANCELLED".equals(t.getStatus()))
    .map(t -> java.math.BigDecimal.valueOf(t.getJourney().getFare())).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
%>
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Group booking | TrackEase</title><jsp:include page="../WEB-INF/ui/head.jsp" /></head>
<body><jsp:include page="../navbar.jsp" />
<main id="main-content" class="shell">
<div class="page-intro section-heading"><div><span class="eyebrow">One booking. Everyone together.</span><h1>Your passenger tickets.</h1>
<p><%= confirmed %> confirmed · <%= waiting %> waitlisted · <%= rac %> RAC · <%= cancelled %> cancelled</p></div>
<a class="text-link" href="${esc:html(pageContext.request.contextPath)}/user/tickets">My tickets ↗</a></div>
<article class="panel group-confirmation">
<div class="section-heading"><div><span class="eyebrow">Booking reference</span><code class="group-reference"><%= com.trainticket.util.Html.escape(bookingId) %></code></div>
<span class="badge badge-confirmed"><%= passengers.size() %> passenger<%= passengers.size()==1?"":"s" %></span></div>
<h2><%= com.trainticket.util.Html.escape(first.getJourney().getTrain().getTrainName()) %></h2>
<p><%= com.trainticket.util.Html.escape(first.getJourney().getDepartureStation().getStationName()) %> → <%= com.trainticket.util.Html.escape(first.getJourney().getArrivalStation().getStationName()) %><br>
<%= com.trainticket.util.Html.escape(first.getJourney().getDepartureTime().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm"))) %></p>
<% if(waiting>0) { %><div class="alert alert-info" role="status">Some passengers are on the waiting list and do not have reserved seats. Their tickets update automatically when seats become available. Check individual statuses before travelling.</div><% } %>
<div class="group-ticket-list">
<% for(Ticket passenger:passengers) { %>
<section class="group-ticket" data-group-ticket="<%= passenger.getTicketId() %>">
<div><small>PASSENGER · TICKET #<%= passenger.getTicketId() %></small><h3><%= com.trainticket.util.Html.escape(passenger.getPassengerName()) %></h3><small><%= com.trainticket.util.Html.escape(passenger.getTravelClass()) %></small></div>
<div><span class="badge <%= "CONFIRMED".equals(passenger.getStatus())?"badge-confirmed":"CANCELLED".equals(passenger.getStatus())?"badge-cancelled":"badge-pending" %>"><%= com.trainticket.util.Html.escape(passenger.getStatus()) %></span>
<p class="mb-0 mt-2"><% if("WAITLISTED".equals(passenger.getStatus())) { %>Waiting #<%= passenger.getWaitingPosition() %><% } else if("CANCELLED".equals(passenger.getStatus())) { %>No active reservation<% } else { %><%= "RAC".equals(passenger.getStatus())?"Shared berth":"Seat" %> <strong><%= com.trainticket.util.Html.escape(passenger.getSeatNumber()) %></strong> · Coach <strong><%= com.trainticket.util.Html.escape(passenger.getCoachNumber()) %></strong><% } %></p></div>
<a class="button secondary small" href="${esc:html(pageContext.request.contextPath)}/user/ticketConfirmation.jsp?ticketId=<%= passenger.getTicketId() %>">View ticket ↗</a>
</section><% } %>
</div><div class="group-total"><span>Recorded fare for active tickets<small>Includes waitlisted passengers · No payment collected</small></span><strong>₹<%= com.trainticket.util.Html.escape(String.format(java.util.Locale.ENGLISH,"%,.2f",total)) %></strong></div>
<p class="field-help">Manage or cancel individual passenger tickets from My tickets. Cancelling one passenger does not cancel the rest of the group. Seats together are not guaranteed.</p>
<div class="ticket-actions"><a class="button" href="${esc:html(pageContext.request.contextPath)}/api/bookings/group/<%= bookingId %>/pdf">Download booking PDF</a><a class="button" href="${esc:html(pageContext.request.contextPath)}/user/tickets">Manage tickets →</a><button class="button secondary" onclick="window.print()" type="button">Print booking</button></div>
</article><p class="ticket-disclaimer">TrackEase portfolio demonstration · These are not official railway travel documents.</p>
</main><jsp:include page="../WEB-INF/ui/footer.jsp" /></body></html>
