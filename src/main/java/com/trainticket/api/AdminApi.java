package com.trainticket.api;

import com.trainticket.dao.*;
import com.trainticket.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;

@RestController @RequestMapping("/api/admin") @Validated
public class AdminApi {
    private final JdbcTemplate jdbc;
    public AdminApi(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record TrainInput(@NotBlank @Size(max=30) String trainNumber,@NotBlank @Size(max=200) String trainName,@Min(1) @Max(2000) int totalSeats) { }
    public record StationInput(@NotBlank @Pattern(regexp="[A-Za-z0-9]{2,20}") String stationCode,@NotBlank @Size(max=200) String stationName,
        @NotBlank @Size(max=100) String city,@NotBlank @Size(max=100) String state) { }
    public record JourneyInput(@Positive int trainId,@Positive int departureStationId,@Positive int arrivalStationId,
        @NotNull LocalDateTime departureTime,@NotNull LocalDateTime arrivalTime,@NotNull @DecimalMin("0.01") BigDecimal fare,@Min(0) @Max(2000) int availableSeats) { }
    private void success(boolean result) { if(!result) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Record missing, archived, duplicate, or in use"); } }
    private Train train(TrainInput input,int id) {
        Train train=new Train(id,input.trainNumber(),input.trainName(),input.totalSeats()); return train;
    }
    private Station station(StationInput input,int id) {
        return new Station(id,input.stationCode().toUpperCase(Locale.ROOT),input.stationName(),input.city(),input.state());
    }
    private Journey journey(JourneyInput input,int id) {
        if(input.departureStationId()==input.arrivalStationId() || !input.arrivalTime().isAfter(input.departureTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Arrival must follow departure and stations must differ");
        }
        Journey journey=new Journey(); journey.setJourneyId(id); journey.setTrainId(input.trainId());
        journey.setDepartureStationId(input.departureStationId()); journey.setArrivalStationId(input.arrivalStationId());
        journey.setDepartureTime(java.sql.Timestamp.valueOf(input.departureTime())); journey.setArrivalTime(java.sql.Timestamp.valueOf(input.arrivalTime()));
        journey.setFare(input.fare().doubleValue()); journey.setAvailableSeats(input.availableSeats()); return journey;
    }
    @GetMapping("/trains") public List<Train> trains() { return new TrainDAO().getAllTrains(); }
    @PostMapping("/trains") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> addTrain(@Valid @RequestBody TrainInput input,HttpServletRequest req) throws SQLException {
        success(new TrainDAO().addTrain(train(input,0),AuthApi.user(req.getSession(false)).getUserId())); return Map.of("message","Train created");
    }
    @PutMapping("/trains/{id}") public Map<String,String> updateTrain(@PathVariable @Positive int id,@Valid @RequestBody TrainInput input) throws SQLException {
        success(new TrainDAO().updateTrain(train(input,id))); return Map.of("message","Train updated");
    }
    @DeleteMapping("/trains/{id}") public Map<String,String> archiveTrain(@PathVariable @Positive int id) throws SQLException {
        success(new TrainDAO().deleteTrain(id)); return Map.of("message","Train archived; historical bookings retained");
    }
    @GetMapping("/stations") public List<Station> stations() { return new StationDAO().getAllStations(); }
    @PostMapping("/stations") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> addStation(@Valid @RequestBody StationInput input) throws SQLException {
        success(new StationDAO().addStation(station(input,0))); return Map.of("message","Station created");
    }
    @PutMapping("/stations/{id}") public Map<String,String> updateStation(@PathVariable @Positive int id,@Valid @RequestBody StationInput input) throws SQLException {
        success(new StationDAO().updateStation(station(input,id))); return Map.of("message","Station updated");
    }
    @DeleteMapping("/stations/{id}") public Map<String,String> archiveStation(@PathVariable @Positive int id) throws SQLException {
        success(new StationDAO().deleteStation(id)); return Map.of("message","Station archived; historical bookings retained");
    }
    @GetMapping("/journeys") public List<Journey> journeys() { return new JourneyDAO().getAllJourneys(); }
    @PostMapping("/journeys") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> addJourney(@Valid @RequestBody JourneyInput input) throws SQLException {
        success(new JourneyDAO().addJourney(journey(input,0))); return Map.of("message","Journey created");
    }
    @PutMapping("/journeys/{id}") public Map<String,String> updateJourney(@PathVariable @Positive int id,@Valid @RequestBody JourneyInput input) throws SQLException {
        success(new JourneyDAO().updateJourney(journey(input,id))); return Map.of("message","Journey updated");
    }
    @DeleteMapping("/journeys/{id}") public Map<String,String> deleteJourney(@PathVariable @Positive int id) throws SQLException {
        success(new JourneyDAO().deleteJourney(id)); return Map.of("message","Unused journey deleted");
    }
    @GetMapping("/analytics") public Map<String,Object> analytics() throws SQLException {
        Map<String,Object> result=new LinkedHashMap<>(new AnalyticsDAO().summary());
        result.put("dailyBookings",jdbc.queryForList("SELECT DATE(booking_date) AS day,COUNT(*) AS bookings FROM tickets WHERE booking_date>=CURRENT_DATE-INTERVAL '30 days' GROUP BY DATE(booking_date) ORDER BY day"));
        result.put("occupancy",jdbc.queryForList("SELECT j.journey_id,t.train_number,COUNT(k.ticket_id) FILTER(WHERE k.status='CONFIRMED') AS confirmed,j.available_seats FROM journeys j JOIN trains t ON t.train_id=j.train_id LEFT JOIN tickets k ON k.journey_id=j.journey_id GROUP BY j.journey_id,t.train_number ORDER BY j.journey_id DESC LIMIT 100"));
        return result;
    }
    @GetMapping("/bookings") public Map<String,Object> bookings(@RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return Map.of("items",jdbc.queryForList("SELECT ticket_id,user_id,journey_id,status,seat_number,coach_number,booked_fare,booking_date FROM tickets ORDER BY ticket_id DESC LIMIT ? OFFSET ?",size,(long)page*size),
            "page",page,"size",size,"total",jdbc.queryForObject("SELECT COUNT(*) FROM tickets",Long.class));
    }
    @GetMapping("/bookings/{id}/events") public List<Map<String,Object>> events(@PathVariable @Positive int id) {
        return jdbc.queryForList("SELECT event_id,action,actor_id,created_at FROM booking_events WHERE ticket_id=? ORDER BY event_id",id);
    }
}
