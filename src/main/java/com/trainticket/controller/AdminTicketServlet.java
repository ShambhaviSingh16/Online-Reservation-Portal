package com.trainticket.controller;
import com.trainticket.model.Ticket;
import com.trainticket.model.User;
import com.trainticket.dao.TicketDAO;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/admin/tickets")
public class AdminTicketServlet extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
	        throws ServletException, IOException {
	    
		HttpSession session = request.getSession(false);
        if (session == null || !((User) session.getAttribute("user")).getRole().equals("admin")) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
        try {
            TicketDAO ticketDAO = new TicketDAO();
            List<Ticket> tickets = ticketDAO.getAllTickets();
            System.out.println("Total tickets: " + tickets.size()); // Debug line
            request.setAttribute("tickets", tickets);
            request.getRequestDispatcher("/admin/viewTickets.jsp").forward(request, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("error", "Error loading tickets: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/adminDashboard.jsp");
        }
    }
}