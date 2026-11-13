package com.trainticket.controller;

import com.trainticket.dao.TicketDAO;
import com.trainticket.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/user/cancelTicket")
public class CancelTicketServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res) throws IOException {
        User user=(User)req.getSession().getAttribute("user");
        if (user==null) { res.sendError(401); return; }
        try {
            TicketDAO.CancellationResult result=new TicketDAO().cancelTicket(Integer.parseInt(req.getParameter("ticketId")),user.getUserId());
            if (result==TicketDAO.CancellationResult.NOT_FOUND) { res.sendError(404); return; }
            req.getSession().setAttribute("ticketMessage",switch(result) {
                case CANCELLED -> "Ticket cancelled. Any released seat was offered to the waiting list.";
                case ALREADY_CANCELLED -> "This ticket was already cancelled.";
                case DEPARTED -> "Tickets cannot be cancelled after departure.";
                default -> "Cancellation could not be completed. Please try again.";
            });
            res.sendRedirect(req.getContextPath()+"/user/tickets");
        } catch (NumberFormatException e) { res.sendError(400); }
    }
}
