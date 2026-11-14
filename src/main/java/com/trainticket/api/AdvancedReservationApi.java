package com.trainticket.api;

import com.trainticket.dao.*;
import com.trainticket.model.Ticket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController @Validated
public class AdvancedReservationApi {
    private final JdbcTemplate jdbc;private final AdvancedReservationDAO dao=new AdvancedReservationDAO();
    public AdvancedReservationApi(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public record Plan(@NotNull @Size(min=2,max=30) List<AdvancedReservationDAO.Stop> stops,@NotNull @Size(min=1,max=30) List<AdvancedReservationDAO.Coach> coaches){}
    public record Hold(@Positive int journeyId,@Min(0) int from,@Min(1) int to,@NotBlank String travelClass,@NotNull @Size(min=1,max=6) List<String> seats){}
    public record Reserve(@Positive int journeyId,@Min(0) int from,@Min(1) int to,@NotBlank String travelClass,
        @NotNull @Size(min=1,max=6) List<@NotBlank @Size(max=100) String> passengers,@NotBlank String preference,
        boolean allowRac,boolean joinWaitingList,@NotNull UUID requestId,UUID holdId){}
    @PutMapping("/api/admin/journeys/{id}/plan") public Map<String,String> configure(@PathVariable @Positive int id,@Valid @RequestBody Plan input) throws SQLException {
        dao.configure(id,input.stops(),input.coaches());return Map.of("message","Route, classes and coaches configured");
    }
    @GetMapping("/api/journeys/{id}/plan") public Map<String,Object> plan(@PathVariable @Positive int id) {
        List<Map<String,Object>> stops=jdbc.queryForList("SELECT js.stop_index,js.station_id,s.station_name,s.station_code,js.arrival_time,js.departure_time,js.cumulative_fare FROM journey_stops js JOIN stations s ON s.station_id=js.station_id WHERE js.journey_id=? ORDER BY stop_index",id);
        if(stops.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Journey has no advanced route plan");
        return Map.of("stops",stops,"classes",jdbc.queryForList("SELECT travel_class,fare_multiplier FROM journey_classes WHERE journey_id=? ORDER BY fare_multiplier",id));
    }
    @GetMapping("/api/journeys/segments") public List<Map<String,Object>> search(@RequestParam @Positive int departureStation,@RequestParam @Positive int arrivalStation,@RequestParam LocalDate travelDate) {
        return jdbc.queryForList("SELECT j.journey_id,t.train_name,t.train_number,a.stop_index AS segment_start,b.stop_index AS segment_end,ds.station_name AS departure,ar.station_name AS arrival,a.departure_time,b.arrival_time,cl.travel_class,ROUND((b.cumulative_fare-a.cumulative_fare)*cl.fare_multiplier,2) AS fare FROM journeys j JOIN trains t ON t.train_id=j.train_id JOIN journey_stops a ON a.journey_id=j.journey_id JOIN journey_stops b ON b.journey_id=j.journey_id AND b.stop_index>a.stop_index JOIN journey_classes cl ON cl.journey_id=j.journey_id JOIN stations ds ON ds.station_id=a.station_id JOIN stations ar ON ar.station_id=b.station_id WHERE j.advanced_reservations AND t.is_active AND ds.is_active AND ar.is_active AND a.station_id=? AND b.station_id=? AND DATE(a.departure_time)=? AND a.departure_time>CURRENT_TIMESTAMP ORDER BY a.departure_time,fare",departureStation,arrivalStation,travelDate);
    }
    @GetMapping("/api/journeys/{id}/seats") public Map<String,Object> seats(@PathVariable @Positive int id,@RequestParam @Min(0) int from,@RequestParam @Min(1) int to,@RequestParam String travelClass,HttpServletRequest req) throws SQLException {
        return dao.seatMap(id,from,to,travelClass,AuthApi.user(req.getSession(false)).getUserId());
    }
    @PostMapping("/api/bookings/holds") public Map<String,Object> hold(@Valid @RequestBody Hold input,HttpServletRequest req) throws SQLException {
        return dao.hold(AuthApi.user(req.getSession(false)).getUserId(),input.journeyId(),input.from(),input.to(),input.travelClass(),input.seats());
    }
    @GetMapping("/api/bookings/holds/active") public List<Map<String,Object>> activeHolds(@RequestParam @Positive int journeyId,HttpServletRequest req) {
        return jdbc.query("SELECT h.*,COUNT(DISTINCT o.coach_number||':'||o.seat_number) AS seat_count FROM seat_holds h JOIN seat_occupancy o ON o.hold_id=h.hold_id WHERE h.user_id=? AND h.journey_id=? AND h.state='ACTIVE' AND h.expires_at>CURRENT_TIMESTAMP GROUP BY h.hold_id ORDER BY h.created_at DESC",
            (r,n)->Map.of("holdId",r.getObject("hold_id").toString(),"expiresAt",r.getTimestamp("expires_at").toInstant().toString(),"from",r.getInt("segment_start"),"to",r.getInt("segment_end"),"travelClass",r.getString("travel_class"),"passengers",r.getInt("seat_count")),AuthApi.user(req.getSession(false)).getUserId(),journeyId);
    }
    @DeleteMapping("/api/bookings/holds/{id}") public Map<String,String> release(@PathVariable UUID id,HttpServletRequest req) throws SQLException {
        if(!dao.release(AuthApi.user(req.getSession(false)).getUserId(),id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Hold not found");
        return Map.of("message","Hold released");
    }
    @PostMapping("/api/bookings/advanced") public Map<String,Object> book(@Valid @RequestBody Reserve input,HttpServletRequest req) throws SQLException {
        int user=AuthApi.user(req.getSession(false)).getUserId();
        dao.book(user,new AdvancedReservationDAO.Booking(input.journeyId(),input.from(),input.to(),input.travelClass(),input.passengers(),input.preference(),input.allowRac(),input.joinWaitingList(),input.requestId(),input.holdId()));
        List<Ticket> tickets=new TicketDAO().getBookingGroup(user,input.requestId().toString());return Map.of("bookingId",input.requestId(),"tickets",tickets);
    }
}
