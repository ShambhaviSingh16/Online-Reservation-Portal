package com.trainticket.service;

import com.trainticket.dao.TicketDAO;
import com.trainticket.model.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ReservationService {
    private final TicketDAO tickets=new TicketDAO();
    public Ticket book(int user,int journey,String request,boolean waiting) {
        Ticket ticket=new Ticket(); ticket.setUserId(user); ticket.setJourneyId(journey);
        TicketDAO.BookingResult result=tickets.reserveTicket(ticket,request,waiting);
        switch(result) {
            case INVALID -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid booking request");
            case UNAVAILABLE -> throw new ResponseStatusException(HttpStatus.CONFLICT,"No available seat or journey departed");
            case FAILED -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Booking unavailable");
            default -> { return ownTicket(user,ticket.getTicketId()); }
        }
    }
    public Ticket ownTicket(int user,int id) {
        Ticket ticket=tickets.getTicketById(id);
        if(ticket==null || ticket.getUserId()!=user) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found"); }
        return ticket;
    }
    public Ticket cancel(int user,int id) {
        switch(tickets.cancelTicket(id,user)) {
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found");
            case DEPARTED -> throw new ResponseStatusException(HttpStatus.CONFLICT,"Journey has departed");
            case FAILED -> throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cancellation unavailable");
            default -> { return ownTicket(user,id); }
        }
    }
}
