package com.trainticket.api;

import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Whitelisted queries provide server-side search, filtering and pagination. */
@RestController @RequestMapping("/api/admin/catalog") @Validated
public class AdminCatalogApi {
    private final JdbcTemplate jdbc;
    private final com.trainticket.service.CatalogService catalog;
    public AdminCatalogApi(JdbcTemplate jdbc,com.trainticket.service.CatalogService catalog){this.jdbc=jdbc;this.catalog=catalog;}
    @GetMapping public Map<String,Object> search(@RequestParam(defaultValue="bookings") String type,
            @RequestParam(defaultValue="") @Size(max=100) String q,@RequestParam(defaultValue="all") String filter,
            @RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="10") @Min(1) @Max(100) int size) {
        if(type.equals("trains") || type.equals("stations")) return catalog.search(type,q,filter,page,size);
        String select,from,search,condition="TRUE",order;
        switch(type) {
            case "journeys" -> {select="j.journey_id AS id,t.train_name,ds.station_code AS departure,ar.station_code AS arrival,j.departure_time,j.arrival_time,j.fare,j.available_seats";
                from="journeys j JOIN trains t ON t.train_id=j.train_id JOIN stations ds ON ds.station_id=j.departure_station_id JOIN stations ar ON ar.station_id=j.arrival_station_id";
                search="concat_ws(' ',j.journey_id,t.train_name,t.train_number,ds.station_name,ar.station_name)";order="j.departure_time";
                if(!Set.of("all","upcoming","departed").contains(filter))throw invalid();if(!filter.equals("all"))condition="j.departure_time"+(filter.equals("upcoming")?">":"<=")+"CURRENT_TIMESTAMP";}
            case "bookings" -> {select="k.ticket_id AS id,COALESCE(k.passenger_name,u.full_name) AS passenger,u.username,t.train_name,k.status,k.coach_number,k.seat_number,k.booked_fare,k.booking_date";
                from="tickets k JOIN users u ON u.user_id=k.user_id JOIN journeys j ON j.journey_id=k.journey_id JOIN trains t ON t.train_id=j.train_id";
                search="concat_ws(' ',k.ticket_id,k.passenger_name,u.full_name,u.username,t.train_name,t.train_number,k.status)";order="k.ticket_id";
                if(!Set.of("all","CONFIRMED","WAITLISTED","CANCELLED","RAC").contains(filter))throw invalid();if(!filter.equals("all"))condition="k.status='"+filter+"'";}
            default -> throw invalid();
        }
        String where=" WHERE ("+condition+") AND "+search+" ILIKE ?";
        String term="%"+q.strip().replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM "+from+where,Long.class,term);
        return Map.of("items",jdbc.queryForList("SELECT "+select+" FROM "+from+where+" ORDER BY "+order+" DESC LIMIT ? OFFSET ?",term,size,(long)page*size),
            "page",page,"size",size,"total",total==null?0:total,"type",type);
    }
    private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported resource or filter");}
}
