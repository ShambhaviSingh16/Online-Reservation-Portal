package com.trainticket.controller;
import com.trainticket.model.Ticket;
import com.trainticket.dao.TicketDAO;
import com.trainticket.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/user/tickets")
public class UserTicketServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
        
        try {
            User user = (User) session.getAttribute("user");
            TicketDAO ticketDAO = new TicketDAO();
            System.out.println("User ID: " + user.getUserId());
            List<Ticket> tickets = ticketDAO.getTicketsByUser(user.getUserId());
            System.out.println("Tickets fetched: " + tickets.size()); // Debug line
            
            request.setAttribute("tickets", tickets);
            request.getRequestDispatcher("/user/myTickets.jsp").forward(request, response);
            
            
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("error", "Error loading tickets: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/user/userDashboard.jsp");
            
            
        }
    }
}