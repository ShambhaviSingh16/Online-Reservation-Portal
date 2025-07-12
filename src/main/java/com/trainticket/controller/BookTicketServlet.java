package com.trainticket.controller;

import com.trainticket.dao.JourneyDAO;
import com.trainticket.dao.TicketDAO;
import com.trainticket.model.Ticket;
import com.trainticket.model.User;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// Use ABSOLUTE path starting with "/"
@WebServlet("/user/bookTicket")
public class BookTicketServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        System.out.println("BookTicketServlet reached!"); // Debug line
        
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
        try {
            int journeyId = Integer.parseInt(request.getParameter("journeyId"));
            String seatNumber = request.getParameter("seatNumber").trim().toUpperCase();
            String coachNumber = request.getParameter("coachNumber").trim().toUpperCase();
            User user = (User) session.getAttribute("user");
            
            System.out.println("Booking ticket for journey: " + journeyId); // Debug line
            System.out.println("[DEBUG] JourneyID: " + journeyId 
            	    + ", Seat: " + seatNumber + ", Coach: " + coachNumber);
            
            TicketDAO ticketDAO = new TicketDAO();
            
            // Check seat availability
            if (!ticketDAO.isSeatAvailable(journeyId, seatNumber, coachNumber)) {
                session.setAttribute("error", "Seat " + seatNumber + " in Coach " + coachNumber + " is already booked!");
                
                response.sendRedirect(request.getContextPath() + "/user/bookTicket.jsp");
                return;
            }
            
            // Proceed with booking          
            Ticket ticket = new Ticket();
            ticket.setJourneyId(journeyId);
            ticket.setUserId(user.getUserId());
            ticket.setSeatNumber(seatNumber);
            ticket.setCoachNumber(coachNumber);
            
            
            JourneyDAO journeyDAO = new JourneyDAO();
            
            // Start transaction
            boolean ticketBooked = ticketDAO.bookTicket(ticket);
            boolean seatsUpdated = journeyDAO.updateAvailableSeats(journeyId, 1);
            
            if (ticketBooked && seatsUpdated) {
            	 response.sendRedirect(request.getContextPath() + 
                         "/user/ticketConfirmation.jsp?ticketId=" + ticket.getTicketId());
            	 System.out.println("Redirecting with ticketId: " + ticket.getTicketId());
            } else {
                session.setAttribute("error", "Booking failed. Please try again.");
                response.sendRedirect(request.getContextPath() + "/user/bookTicket.jsp");
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("error", "Error: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/user/bookTicket.jsp");
        }
    }
}