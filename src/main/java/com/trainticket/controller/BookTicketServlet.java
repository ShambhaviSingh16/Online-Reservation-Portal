package com.trainticket.controller;

import com.trainticket.dao.TicketDAO;
import com.trainticket.dao.TicketDAO.BookingResult;
import com.trainticket.model.Ticket;
import com.trainticket.model.User;
import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/user/bookTicket")
public class BookTicketServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        try {
            String[] passengers=request.getParameterValues("passengerName");
            if(passengers!=null) {
                TicketDAO.GroupBooking booking=new TicketDAO().reserveGroup(
                        ((User)session.getAttribute("user")).getUserId(),Integer.parseInt(request.getParameter("journeyId")),
                        request.getParameter("bookingRequestId"),java.util.Arrays.asList(passengers),
                        "true".equals(request.getParameter("joinWaitingList")));
                if(booking.result()==TicketDAO.GroupResult.BOOKED) {
                    response.sendRedirect(request.getContextPath()+"/user/bookingConfirmation.jsp?bookingId="+booking.tickets().get(0).getBookingGroupId());
                    return;
                }
                session.setAttribute("error",switch(booking.result()) {
                    case INVALID -> "This booking form is invalid. Enter names for 1–6 passengers and try again.";
                    case UNAVAILABLE -> "There are not enough seats for everyone, or this journey is no longer bookable. No tickets were booked. Search again or allow the waiting list.";
                    default -> "Booking could not be completed. No tickets were booked. Please try again later.";
                });
                response.sendRedirect(request.getContextPath()+"/user/bookTicket.jsp");
                return;
            }
            Ticket ticket = new Ticket();
            ticket.setJourneyId(Integer.parseInt(request.getParameter("journeyId")));
            ticket.setUserId(((User) session.getAttribute("user")).getUserId());
            BookingResult result = new TicketDAO().reserveTicket(ticket, request.getParameter("bookingRequestId"), "true".equals(request.getParameter("joinWaitingList")));
            if (result == BookingResult.CONFIRMED || result == BookingResult.WAITLISTED || result == BookingResult.CANCELLED) {
                response.sendRedirect(request.getContextPath() +
                        "/user/ticketConfirmation.jsp?ticketId=" + ticket.getTicketId());
                return;
            }
            String message;
            switch (result) {
                case INVALID: message = "This booking form is invalid. Please search again and retry."; break;
                case UNAVAILABLE: message = "This journey has no seats available or has already departed. Please search again."; break;
                default: message = "Booking could not be completed. Please try again later.";
            }
            session.setAttribute("error", message);
        } catch (NumberFormatException e) {
            session.setAttribute("error", "Please select a valid journey.");
        }
        response.sendRedirect(request.getContextPath() + "/user/bookTicket.jsp");
    }
}
