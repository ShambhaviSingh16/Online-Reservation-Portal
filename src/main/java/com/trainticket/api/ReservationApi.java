package com.trainticket.api;

import com.trainticket.dao.*;
import com.trainticket.model.*;
import com.trainticket.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api") @Validated
public class ReservationApi {
    private final ReservationService service;
    private final JdbcTemplate jdbc;
    public ReservationApi(ReservationService service,JdbcTemplate jdbc) { this.service=service; this.jdbc=jdbc; }
    public record Booking(@Positive int journeyId,@NotNull UUID requestId,boolean joinWaitingList) { }
    public record GroupBookingRequest(@Positive int journeyId,@NotNull UUID requestId,
            @NotNull @Size(min=1,max=6) List<@NotBlank @Size(max=100) String> passengerNames,boolean joinWaitingList) { }
    @io.swagger.v3.oas.annotations.Operation(summary="Book 1–6 named passengers in one atomic submission",
            description="Each passenger receives a ticket under the requestId booking reference. Without waiting-list consent, insufficient seats roll back the whole booking. With consent, some tickets may be confirmed and others waitlisted. Seats together are not guaranteed. Reusing the same requestId and passenger names returns existing tickets.")
    @PostMapping("/bookings/group") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> bookGroup(@Valid @RequestBody GroupBookingRequest body,HttpServletRequest request) {
        int user=AuthApi.user(request.getSession(false)).getUserId();
        TicketDAO.GroupBooking result=new TicketDAO().reserveGroup(user,body.journeyId(),body.requestId().toString(),body.passengerNames(),body.joinWaitingList());
        if(result.result()!=TicketDAO.GroupResult.BOOKED) throw new org.springframework.web.server.ResponseStatusException(
                switch(result.result()) { case INVALID -> HttpStatus.BAD_REQUEST; case UNAVAILABLE -> HttpStatus.CONFLICT; default -> HttpStatus.INTERNAL_SERVER_ERROR; },
                switch(result.result()) { case INVALID -> "Invalid group booking or request key already used with different passengers.";
                    case UNAVAILABLE -> "Not enough seats for the group or journey unavailable. No tickets booked."; default -> "Group booking failed. No tickets booked."; });
        return Map.of("bookingId",body.requestId(),"tickets",new TicketDAO().getBookingGroup(user,body.requestId().toString()));
    }
    @GetMapping("/bookings/group/{bookingId}")
    public Map<String,Object> group(@PathVariable UUID bookingId,HttpServletRequest request) {
        List<Ticket> tickets=new TicketDAO().getBookingGroup(AuthApi.user(request.getSession(false)).getUserId(),bookingId.toString());
        if(tickets.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found.");
        return Map.of("bookingId",bookingId,"tickets",tickets);
    }
    @GetMapping("/stations") public List<Station> stations() { return new StationDAO().getAllStations(); }
    @GetMapping("/journeys/search") public List<Journey> search(@RequestParam @Positive int departureStation,
            @RequestParam @Positive int arrivalStation,@RequestParam LocalDate travelDate) {
        return new JourneyDAO().searchJourneys(departureStation,arrivalStation,java.sql.Date.valueOf(travelDate));
    }
    @PostMapping("/bookings") @ResponseStatus(HttpStatus.CREATED)
    public Ticket book(@Valid @RequestBody Booking body,HttpServletRequest request) {
        return service.book(AuthApi.user(request.getSession(false)).getUserId(),body.journeyId(),body.requestId().toString(),body.joinWaitingList());
    }
    @GetMapping("/bookings/{id}") public Ticket ticket(@PathVariable @Positive int id,HttpServletRequest request) {
        return service.ownTicket(AuthApi.user(request.getSession(false)).getUserId(),id);
    }
    @DeleteMapping("/bookings/{id}") public Ticket cancel(@PathVariable @Positive int id,HttpServletRequest request) {
        return service.cancel(AuthApi.user(request.getSession(false)).getUserId(),id);
    }
    @GetMapping("/bookings/history") public Map<String,Object> history(@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,HttpServletRequest request) {
        int user=AuthApi.user(request.getSession(false)).getUserId();
        // Database pagination avoids reading another user's records or loading full histories.
        List<Map<String,Object>> items=jdbc.queryForList(
            "SELECT ticket_id,journey_id,seat_number,coach_number,status,booking_date,booked_fare,passenger_name,booking_group_id FROM tickets WHERE user_id=? ORDER BY ticket_id DESC LIMIT ? OFFSET ?",
            user,size,Math.multiplyExact((long)page,size));
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM tickets WHERE user_id=?",Long.class,user);
        return Map.of("items",items,"page",page,"size",size,"total",total==null?0:total);
    }
}
