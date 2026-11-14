package com.trainticket.dao;

import com.trainticket.util.DBUtil;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Segment occupancy is represented per leg and half-berth slot, protected by the journey lock. */
public class AdvancedReservationDAO {
    public static final Set<String> CLASSES=Set.of("STANDARD","SLEEPER","AC3","AC2");
    public static final Set<String> PREFERENCES=Set.of("ANY","SEAT","LOWER","MIDDLE","UPPER","SIDE_LOWER","SIDE_UPPER");
    public record Stop(int stationId,LocalDateTime arrival,LocalDateTime departure,BigDecimal cumulativeFare){}
    public record Coach(String coach,String travelClass,int seats,BigDecimal multiplier,int racBerths){}
    public record Booking(int journeyId,int from,int to,String travelClass,List<String> passengers,String preference,
            boolean allowRac,boolean joinWaitingList,UUID requestId,UUID holdId){}
    private record Seat(String coach,String number,String berth,boolean rac,int slot){}
    private record Segment(int journey,int from,int to,String travelClass,BigDecimal fare,int departureStation,int arrivalStation,
            String departureName,String departureCode,String arrivalName,String arrivalCode,Timestamp departure,Timestamp arrival){}
    public static class Conflict extends RuntimeException {public Conflict(String message){super(message);}}

    private void lock(Connection c,int journey) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("SELECT j.advanced_reservations,(tr.is_active AND ds.is_active AND ar.is_active) AS active FROM journeys j JOIN trains tr ON tr.train_id=j.train_id JOIN stations ds ON ds.station_id=j.departure_station_id JOIN stations ar ON ar.station_id=j.arrival_station_id WHERE j.journey_id=? FOR UPDATE OF j")) {
            s.setInt(1,journey);try(ResultSet r=s.executeQuery()) {if(!r.next() || !r.getBoolean(1) || !r.getBoolean(2))throw new Conflict("Journey is not available for advanced reservations");}
        }
    }
    public void configure(int journey,List<Stop> stops,List<Coach> coaches) throws SQLException {
        if(stops==null || stops.size()<2 || stops.size()>30 || coaches==null || coaches.isEmpty() || coaches.size()>30)throw new IllegalArgumentException("Provide 2–30 stops and 1–30 coaches");
        Set<Integer> stations=new HashSet<>();Set<String> labels=new HashSet<>();int count=0;BigDecimal previous=BigDecimal.ZERO;
        LocalDateTime last=null;Map<String,BigDecimal> multipliers=new HashMap<>();
        for(int i=0;i<stops.size();i++) {
            Stop stop=stops.get(i);
            if(stop==null || stop.arrival()==null || stop.departure()==null || stop.departure().isBefore(stop.arrival()) || stop.cumulativeFare()==null || stop.cumulativeFare().scale()>2
                    || !stations.add(stop.stationId()) || (i>0 && stop.cumulativeFare().compareTo(previous)<=0) || (i==0 && stop.cumulativeFare().signum()!=0)
                    || (last!=null && !stop.arrival().isAfter(last)))throw new IllegalArgumentException("Stops must be unique, chronological, and have increasing cumulative fares starting at zero");
            previous=stop.cumulativeFare();last=stop.departure();
        }
        if(previous.signum()<=0)throw new IllegalArgumentException("Route fare must be positive");
        for(Coach coach:coaches) {
            if(coach==null || coach.coach()==null || !coach.coach().matches("[A-Z][A-Z0-9]{0,9}") || !labels.add(coach.coach()) || !CLASSES.contains(coach.travelClass())
                    || coach.seats()<1 || coach.seats()>80 || coach.multiplier()==null || coach.multiplier().signum()<=0 || coach.multiplier().compareTo(BigDecimal.valueOf(100))>0 || coach.multiplier().scale()>2
                    || coach.racBerths()<0 || coach.racBerths()>coach.seats()/8 || (coach.travelClass().equals("STANDARD") && coach.racBerths()!=0))throw new IllegalArgumentException("Invalid class, coach capacity, multiplier, or RAC quota");
            if(multipliers.containsKey(coach.travelClass()) && multipliers.get(coach.travelClass()).compareTo(coach.multiplier())!=0)throw new IllegalArgumentException("Each class needs one fare multiplier");
            multipliers.put(coach.travelClass(),coach.multiplier());count+=coach.seats();
        }
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement s=c.prepareStatement("SELECT j.*,t.total_seats FROM journeys j JOIN trains t ON t.train_id=j.train_id WHERE j.journey_id=? AND t.is_active FOR UPDATE OF j")) {
                    s.setInt(1,journey);try(ResultSet r=s.executeQuery()) {
                        if(!r.next() || !r.getTimestamp("departure_time").after(new java.util.Date()))throw new Conflict("Configure a future active journey");
                        if(count>r.getInt("total_seats"))throw new IllegalArgumentException("Coach seats exceed train capacity");
                        if(stops.get(0).stationId()!=r.getInt("departure_station_id") || stops.get(stops.size()-1).stationId()!=r.getInt("arrival_station_id")
                                || !stops.get(0).departure().equals(r.getTimestamp("departure_time").toLocalDateTime())
                                || !stops.get(stops.size()-1).arrival().equals(r.getTimestamp("arrival_time").toLocalDateTime()))throw new IllegalArgumentException("First/last stops and times must match the journey");
                    }
                }
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM tickets WHERE journey_id=? UNION ALL SELECT 1 FROM seat_holds WHERE journey_id=? LIMIT 1")) {
                    s.setInt(1,journey);s.setInt(2,journey);try(ResultSet r=s.executeQuery()){if(r.next())throw new Conflict("Cannot replace the route or coaches after reservations or holds exist");}
                }
                for(String table:List.of("journey_seats","journey_stops","journey_classes"))try(PreparedStatement s=c.prepareStatement("DELETE FROM "+table+" WHERE journey_id=?")){s.setInt(1,journey);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("INSERT INTO journey_stops VALUES(?,?,?,?,?,?)")) {
                    for(int i=0;i<stops.size();i++){Stop stop=stops.get(i);s.setInt(1,journey);s.setInt(2,i);s.setInt(3,stop.stationId());s.setTimestamp(4,Timestamp.valueOf(stop.arrival()));s.setTimestamp(5,Timestamp.valueOf(stop.departure()));s.setBigDecimal(6,stop.cumulativeFare());s.addBatch();}s.executeBatch();
                }
                try(PreparedStatement s=c.prepareStatement("INSERT INTO journey_classes VALUES(?,?,?)")){
                    for(var entry:multipliers.entrySet()){s.setInt(1,journey);s.setString(2,entry.getKey());s.setBigDecimal(3,entry.getValue());s.addBatch();}s.executeBatch();
                }
                int position=0;
                try(PreparedStatement s=c.prepareStatement("INSERT INTO journey_seats(journey_id,position,coach_number,seat_number,travel_class,berth_type,rac_enabled) VALUES(?,?,?,?,?,?,?)")) {
                    for(Coach coach:coaches)for(int n=1;n<=coach.seats();n++) {
                        String berth=coach.travelClass().equals("STANDARD")?"SEAT":switch((n-1)%8){case 0,3->"LOWER";case 1,4->"MIDDLE";case 2,5->"UPPER";case 6->"SIDE_LOWER";default->"SIDE_UPPER";};
                        // AC2 has no middle berth; positions 2 and 5 are upper in this simplified layout.
                        if(coach.travelClass().equals("AC2") && berth.equals("MIDDLE"))berth="UPPER";
                        s.setInt(1,journey);s.setInt(2,++position);s.setString(3,coach.coach());s.setString(4,""+n);s.setString(5,coach.travelClass());s.setString(6,berth);
                        s.setBoolean(7,berth.equals("SIDE_LOWER") && ((n-1)/8)<coach.racBerths());s.addBatch();
                    }s.executeBatch();
                }
                try(PreparedStatement s=c.prepareStatement("UPDATE journeys SET advanced_reservations=TRUE,fare=? WHERE journey_id=?")){s.setBigDecimal(1,previous);s.setInt(2,journey);s.executeUpdate();}
                sync(c,journey);c.commit();
            }catch(SQLException | RuntimeException e){c.rollback();throw e;}
        }
    }
    private Segment segment(Connection c,int journey,int from,int to,String travelClass,boolean future) throws SQLException {
        if(from<0 || to<=from || !CLASSES.contains(travelClass))throw new IllegalArgumentException("Invalid segment or class");
        try(PreparedStatement s=c.prepareStatement("SELECT a.station_id AS dep,b.station_id AS arr,ds.station_name AS dn,ds.station_code AS dc,ar.station_name AS an,ar.station_code AS ac,a.departure_time,b.arrival_time,(b.cumulative_fare-a.cumulative_fare)*cl.fare_multiplier AS fare FROM journey_stops a JOIN journey_stops b ON b.journey_id=a.journey_id JOIN journey_classes cl ON cl.journey_id=a.journey_id JOIN stations ds ON ds.station_id=a.station_id JOIN stations ar ON ar.station_id=b.station_id WHERE a.journey_id=? AND a.stop_index=? AND b.stop_index=? AND cl.travel_class=? AND (?=FALSE OR (ds.is_active AND ar.is_active))")) {
            s.setInt(1,journey);s.setInt(2,from);s.setInt(3,to);s.setString(4,travelClass);s.setBoolean(5,future);
            try(ResultSet r=s.executeQuery()) {
                if(!r.next())throw new Conflict("Segment or travel class unavailable");
                if(future && !r.getTimestamp("departure_time").after(new java.util.Date()))throw new Conflict("Boarding time has passed");
                BigDecimal fare=r.getBigDecimal("fare").setScale(2,java.math.RoundingMode.HALF_UP);if(fare.signum()<=0)throw new Conflict("Segment fare must be positive");
                return new Segment(journey,from,to,travelClass,fare,r.getInt("dep"),r.getInt("arr"),r.getString("dn"),r.getString("dc"),r.getString("an"),r.getString("ac"),r.getTimestamp("departure_time"),r.getTimestamp("arrival_time"));
            }
        }
    }
    private void expire(Connection c,int journey) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("DELETE FROM seat_occupancy WHERE hold_id IN(SELECT hold_id FROM seat_holds WHERE journey_id=? AND state='ACTIVE' AND expires_at<=CURRENT_TIMESTAMP)")){s.setInt(1,journey);s.executeUpdate();}
        try(PreparedStatement s=c.prepareStatement("UPDATE seat_holds SET state='EXPIRED' WHERE journey_id=? AND state='ACTIVE' AND expires_at<=CURRENT_TIMESTAMP")){s.setInt(1,journey);s.executeUpdate();}
    }
    private Seat free(Connection c,Segment seg,String preference,boolean rac,String exact) throws SQLException {
        String sql="SELECT s.*,n.slot FROM journey_seats s CROSS JOIN generate_series(0,1) n(slot) WHERE s.journey_id=? AND s.travel_class=? AND s.rac_enabled=? "
            +"AND (n.slot=0 OR ?) AND NOT EXISTS(SELECT 1 FROM seat_occupancy o WHERE o.journey_id=s.journey_id AND o.coach_number=s.coach_number AND o.seat_number=s.seat_number AND o.leg>=? AND o.leg<? AND (NOT ? OR o.slot=n.slot)) "
            +"AND (CAST(? AS TEXT) IS NULL OR s.coach_number||':'||s.seat_number=?) ORDER BY CASE WHEN s.berth_type=? THEN 0 ELSE 1 END,s.position,n.slot LIMIT 1";
        try(PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,seg.journey);s.setString(2,seg.travelClass);s.setBoolean(3,rac);s.setBoolean(4,rac);s.setInt(5,seg.from);s.setInt(6,seg.to);s.setBoolean(7,rac);s.setString(8,exact);s.setString(9,exact);s.setString(10,preference);
            try(ResultSet r=s.executeQuery()){return r.next()?new Seat(r.getString("coach_number"),r.getString("seat_number"),r.getString("berth_type"),rac,r.getInt("slot")):null;}
        }
    }
    private void occupy(Connection c,Segment seg,Seat seat,Integer ticket,UUID hold) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO seat_occupancy(journey_id,coach_number,seat_number,leg,slot,ticket_id,hold_id) VALUES(?,?,?,?,?,?,?)")) {
            for(int leg=seg.from;leg<seg.to;leg++)for(int slot=0;slot<2;slot++)if(!seat.rac || slot==seat.slot) {
                s.setInt(1,seg.journey);s.setString(2,seat.coach);s.setString(3,seat.number);s.setInt(4,leg);s.setInt(5,slot);
                if(ticket==null)s.setNull(6,Types.INTEGER);else s.setInt(6,ticket);s.setObject(7,hold);s.addBatch();
            }s.executeBatch();
        }
    }
    private void audit(Connection c,int ticket,int actor,String action) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO booking_events(ticket_id,actor_id,action) VALUES(?,?,?)")) {
            s.setInt(1,ticket);if(actor<=0)s.setNull(2,Types.INTEGER);else s.setInt(2,actor);s.setString(3,action);s.executeUpdate();
        }
    }
    private void promote(Connection c,int journey) throws SQLException {
        List<Map<String,Object>> pending=new ArrayList<>();
        try(PreparedStatement s=c.prepareStatement("SELECT * FROM tickets WHERE journey_id=? AND advanced AND status IN('RAC','WAITLISTED') AND segment_departure_time>CURRENT_TIMESTAMP ORDER BY CASE WHEN status='RAC' THEN 0 ELSE 1 END,ticket_id FOR UPDATE")) {
            s.setInt(1,journey);try(ResultSet r=s.executeQuery()) {while(r.next())pending.add(Map.of("id",r.getInt("ticket_id"),"from",r.getInt("segment_start"),"to",r.getInt("segment_end"),"class",r.getString("travel_class"),"preference",r.getString("berth_preference"),"rac",r.getBoolean("allow_rac"),"status",r.getString("status")));}
        }
        for(var row:pending) {
            Segment seg=segment(c,journey,(int)row.get("from"),(int)row.get("to"),(String)row.get("class"),false);
            Seat seat=free(c,seg,(String)row.get("preference"),false,null);
            if(seat==null && row.get("status").equals("WAITLISTED") && (boolean)row.get("rac"))seat=free(c,seg,"ANY",true,null);
            if(seat==null)continue;
            int id=(int)row.get("id");
            try(PreparedStatement s=c.prepareStatement("DELETE FROM seat_occupancy WHERE ticket_id=?")){s.setInt(1,id);s.executeUpdate();}
            occupy(c,seg,seat,id,null);
            try(PreparedStatement s=c.prepareStatement("UPDATE tickets SET status=?,coach_number=?,seat_number=? WHERE ticket_id=?")) {
                s.setString(1,seat.rac?"RAC":"CONFIRMED");s.setString(2,seat.coach);s.setString(3,seat.number);s.setInt(4,id);s.executeUpdate();
            }
            audit(c,id,0,seat.rac?"PROMOTED_RAC":"PROMOTED");
        }
    }
    private void sync(Connection c,int journey) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("UPDATE journeys j SET available_seats=(SELECT COUNT(*) FROM journey_seats s WHERE s.journey_id=j.journey_id AND NOT s.rac_enabled AND NOT EXISTS(SELECT 1 FROM seat_occupancy o WHERE o.journey_id=s.journey_id AND o.coach_number=s.coach_number AND o.seat_number=s.seat_number)) WHERE journey_id=?")){s.setInt(1,journey);s.executeUpdate();}
    }
    public Map<String,Object> seatMap(int journey,int from,int to,String travelClass,int user) throws SQLException {
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                lock(c,journey);expire(c,journey);promote(c,journey);Segment seg=segment(c,journey,from,to,travelClass,true);
                List<Map<String,Object>> seats=new ArrayList<>();
                try(PreparedStatement s=c.prepareStatement("SELECT s.*, (SELECT COUNT(DISTINCT o.slot) FROM seat_occupancy o WHERE o.journey_id=s.journey_id AND o.coach_number=s.coach_number AND o.seat_number=s.seat_number AND o.leg>=? AND o.leg<?) AS occupied,(SELECT COUNT(*) FROM seat_occupancy o JOIN seat_holds h ON h.hold_id=o.hold_id WHERE o.journey_id=s.journey_id AND o.coach_number=s.coach_number AND o.seat_number=s.seat_number AND o.leg>=? AND o.leg<? AND h.user_id=?) AS mine FROM journey_seats s WHERE s.journey_id=? AND s.travel_class=? ORDER BY s.position")) {
                    s.setInt(1,from);s.setInt(2,to);s.setInt(3,from);s.setInt(4,to);s.setInt(5,user);s.setInt(6,journey);s.setString(7,travelClass);
                    try(ResultSet r=s.executeQuery()){while(r.next())seats.add(Map.of("key",r.getString("coach_number")+":"+r.getString("seat_number"),"coach",r.getString("coach_number"),"number",r.getString("seat_number"),"berth",r.getString("berth_type"),"rac",r.getBoolean("rac_enabled"),"occupiedSlots",r.getInt("occupied"),"heldByYou",r.getInt("mine")>0));}
                }
                sync(c,journey);c.commit();return Map.of("seats",seats,"fare",seg.fare,"holdSeconds",120,"from",from,"to",to,"travelClass",travelClass);
            }catch(SQLException | RuntimeException e){c.rollback();throw e;}
        }
    }
    public Map<String,Object> hold(int user,int journey,int from,int to,String travelClass,List<String> keys) throws SQLException {
        if(keys==null || keys.isEmpty() || keys.size()>6 || new HashSet<>(keys).size()!=keys.size() || keys.stream().anyMatch(k->k==null || !k.matches("[A-Z][A-Z0-9]{0,9}:[0-9]{1,2}")))throw new IllegalArgumentException("Choose 1–6 distinct seats");
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                lock(c,journey);expire(c,journey);promote(c,journey);Segment seg=segment(c,journey,from,to,travelClass,true);
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM seat_holds WHERE user_id=? AND journey_id=? AND state='ACTIVE' LIMIT 1")) {
                    s.setInt(1,user);s.setInt(2,journey);try(ResultSet r=s.executeQuery()){if(r.next())throw new Conflict("Release your current hold before selecting another set");}
                }
                UUID id=UUID.randomUUID();Instant expires=Instant.now().plusSeconds(120);
                try(PreparedStatement s=c.prepareStatement("INSERT INTO seat_holds(hold_id,user_id,journey_id,segment_start,segment_end,travel_class,expires_at) VALUES(?,?,?,?,?,?,?)")) {
                    s.setObject(1,id);s.setInt(2,user);s.setInt(3,journey);s.setInt(4,from);s.setInt(5,to);s.setString(6,travelClass);s.setTimestamp(7,Timestamp.from(expires));s.executeUpdate();
                }
                for(String key:keys){Seat seat=free(c,seg,"ANY",false,key);if(seat==null)throw new Conflict("A selected seat is no longer available");occupy(c,seg,seat,null,id);}
                sync(c,journey);c.commit();return Map.of("holdId",id,"expiresAt",expires.toString(),"seats",keys);
            }catch(SQLException | RuntimeException e){c.rollback();throw e;}
        }
    }
    public boolean release(int user,UUID id) throws SQLException {
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                int journey;
                try(PreparedStatement s=c.prepareStatement("SELECT journey_id FROM seat_holds WHERE hold_id=? AND user_id=?")){s.setObject(1,id);s.setInt(2,user);try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return false;}journey=r.getInt(1);}}
                lock(c,journey);
                try(PreparedStatement s=c.prepareStatement("DELETE FROM seat_occupancy WHERE hold_id=?")){s.setObject(1,id);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("UPDATE seat_holds SET state='RELEASED' WHERE hold_id=? AND state='ACTIVE'")){s.setObject(1,id);s.executeUpdate();}
                expire(c,journey);promote(c,journey);sync(c,journey);c.commit();return true;
            }catch(SQLException | RuntimeException e){c.rollback();throw e;}
        }
    }
    public List<Integer> book(int user,Booking booking) throws SQLException {
        if(booking==null || booking.requestId==null || booking.passengers==null || booking.passengers.isEmpty() || booking.passengers.size()>6 || !PREFERENCES.contains(booking.preference)
                || booking.passengers.stream().anyMatch(n->n==null || n.strip().isEmpty() || n.length()>100 || n.codePoints().anyMatch(Character::isISOControl)))throw new IllegalArgumentException("Invalid passengers or berth preference");
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement s=c.prepareStatement("SELECT pg_advisory_xact_lock(?)")){s.setLong(1,booking.requestId.getMostSignificantBits()^booking.requestId.getLeastSignificantBits()^user);s.execute();}
                lock(c,booking.journeyId);
                List<Integer> existing=new ArrayList<>();
                try(PreparedStatement s=c.prepareStatement("SELECT * FROM tickets WHERE user_id=? AND booking_group_id=? ORDER BY ticket_id")) {
                    s.setInt(1,user);s.setObject(2,booking.requestId);try(ResultSet r=s.executeQuery()) {while(r.next()) {
                        int i=existing.size();if(!r.getBoolean("advanced") || r.getInt("journey_id")!=booking.journeyId || r.getInt("segment_start")!=booking.from || r.getInt("segment_end")!=booking.to || !r.getString("travel_class").equals(booking.travelClass)
                            || i>=booking.passengers.size() || !r.getString("passenger_name").equals(booking.passengers.get(i).strip()))throw new Conflict("Request reference already belongs to a different booking");
                        existing.add(r.getInt("ticket_id"));
                    }}
                }
                if(!existing.isEmpty()){if(existing.size()!=booking.passengers.size())throw new Conflict("Request passenger count changed");c.rollback();return existing;}
                expire(c,booking.journeyId);promote(c,booking.journeyId);Segment seg=segment(c,booking.journeyId,booking.from,booking.to,booking.travelClass,true);
                List<Seat> held=new ArrayList<>();
                if(booking.holdId!=null) {
                    try(PreparedStatement s=c.prepareStatement("SELECT * FROM seat_holds WHERE hold_id=? AND user_id=? AND state='ACTIVE' AND expires_at>CURRENT_TIMESTAMP FOR UPDATE")) {
                        s.setObject(1,booking.holdId);s.setInt(2,user);try(ResultSet r=s.executeQuery()) {
                            if(!r.next() || r.getInt("journey_id")!=booking.journeyId || r.getInt("segment_start")!=booking.from || r.getInt("segment_end")!=booking.to || !r.getString("travel_class").equals(booking.travelClass))throw new Conflict("Seat hold expired or does not match this booking");
                        }
                    }
                    try(PreparedStatement s=c.prepareStatement("SELECT DISTINCT coach_number,seat_number FROM seat_occupancy WHERE hold_id=? ORDER BY coach_number,seat_number")) {
                        s.setObject(1,booking.holdId);try(ResultSet r=s.executeQuery()){while(r.next())held.add(new Seat(r.getString(1),r.getString(2),"ANY",false,0));}
                    }
                    if(held.size()!=booking.passengers.size())throw new Conflict("The held seat count must match the passenger count");
                    try(PreparedStatement s=c.prepareStatement("DELETE FROM seat_occupancy WHERE hold_id=?")){s.setObject(1,booking.holdId);s.executeUpdate();}
                }
                List<Integer> ids=new ArrayList<>();
                for(int i=0;i<booking.passengers.size();i++) {
                    Seat seat=held.isEmpty()?free(c,seg,booking.preference,false,null):held.get(i);
                    if(seat==null && booking.allowRac)seat=free(c,seg,"ANY",true,null);
                    if(seat==null && !booking.joinWaitingList)throw new Conflict("Not enough available seats or RAC places for everyone. No tickets booked.");
                    String status=seat==null?"WAITLISTED":seat.rac?"RAC":"CONFIRMED";
                    UUID key=i==0?booking.requestId:UUID.nameUUIDFromBytes((booking.requestId+":"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));int id;
                    try(PreparedStatement s=c.prepareStatement("INSERT INTO tickets(journey_id,user_id,seat_number,coach_number,status,booking_request_id,booked_fare,booking_group_id,passenger_name,advanced,travel_class,berth_preference,segment_start,segment_end,allow_rac,segment_departure_name,segment_departure_code,segment_arrival_name,segment_arrival_code,segment_departure_time,segment_arrival_time) VALUES(?,?,?,?,?,?,?,?,?,TRUE,?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                        s.setInt(1,seg.journey);s.setInt(2,user);s.setString(3,seat==null?null:seat.number);s.setString(4,seat==null?null:seat.coach);s.setString(5,status);s.setObject(6,key);s.setBigDecimal(7,seg.fare);s.setObject(8,booking.requestId);s.setString(9,booking.passengers.get(i).strip());
                        s.setString(10,seg.travelClass);s.setString(11,booking.preference);s.setInt(12,seg.from);s.setInt(13,seg.to);s.setBoolean(14,booking.allowRac);
                        s.setString(15,seg.departureName);s.setString(16,seg.departureCode);s.setString(17,seg.arrivalName);s.setString(18,seg.arrivalCode);s.setTimestamp(19,seg.departure);s.setTimestamp(20,seg.arrival);
                        s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Ticket ID missing");id=r.getInt(1);}
                    }
                    if(seat!=null)occupy(c,seg,seat,id,null);audit(c,id,user,status);ids.add(id);
                }
                if(booking.holdId!=null)try(PreparedStatement s=c.prepareStatement("UPDATE seat_holds SET state='CONSUMED' WHERE hold_id=?")){s.setObject(1,booking.holdId);s.executeUpdate();}
                sync(c,seg.journey);c.commit();return ids;
            }catch(SQLException | RuntimeException e){c.rollback();throw e;}
        }
    }
    public TicketDAO.CancellationResult cancel(int user,int ticket) throws SQLException {
        try(Connection c=DBUtil.getConnection()) {
            c.setAutoCommit(false);
            try {
                int journey;
                try(PreparedStatement s=c.prepareStatement("SELECT journey_id FROM tickets WHERE ticket_id=? AND user_id=? AND advanced")){s.setInt(1,ticket);s.setInt(2,user);try(ResultSet r=s.executeQuery()){if(!r.next()){c.rollback();return TicketDAO.CancellationResult.NOT_FOUND;}journey=r.getInt(1);}}
                // Historical archived services must still permit cancellation.
                try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM journeys WHERE journey_id=? FOR UPDATE")){s.setInt(1,journey);s.executeQuery().close();}
                try(PreparedStatement s=c.prepareStatement("SELECT status,segment_departure_time<=CURRENT_TIMESTAMP FROM tickets WHERE ticket_id=? FOR UPDATE")) {
                    s.setInt(1,ticket);try(ResultSet r=s.executeQuery()){r.next();if(r.getString(1).equals("CANCELLED")){c.rollback();return TicketDAO.CancellationResult.ALREADY_CANCELLED;}if(r.getBoolean(2)){c.rollback();return TicketDAO.CancellationResult.DEPARTED;}}
                }
                try(PreparedStatement s=c.prepareStatement("DELETE FROM seat_occupancy WHERE ticket_id=?")){s.setInt(1,ticket);s.executeUpdate();}
                try(PreparedStatement s=c.prepareStatement("UPDATE tickets SET status='CANCELLED',cancelled_at=NOW() WHERE ticket_id=?")){s.setInt(1,ticket);s.executeUpdate();}
                expire(c,journey);promote(c,journey);sync(c,journey);audit(c,ticket,user,"CANCELLED");c.commit();return TicketDAO.CancellationResult.CANCELLED;
            }catch(SQLException | RuntimeException e){c.rollback();throw e;}
        }
    }
}
