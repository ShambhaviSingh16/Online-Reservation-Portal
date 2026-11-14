package com.trainticket.service;

import com.trainticket.dao.TicketDAO;
import com.trainticket.model.Ticket;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import com.trainticket.util.DBUtil;

@Service @EnableScheduling
public class EmailDeliveryService {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from,base,host,provider,brevoKey;
    private final ObjectMapper mapper;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public EmailDeliveryService(ObjectProvider<JavaMailSender> sender,@Value("${app.mail.from:}") String from,@Value("${app.public-base-url:http://localhost:8080}") String base,@Value("${spring.mail.host:}") String host,
            @Value("${app.mail.provider:none}") String provider,@Value("${app.mail.brevo-api-key:}") String brevoKey,ObjectMapper mapper) {
        this.sender=sender;this.from=from;this.base=base.replaceAll("/+$","");this.host=host;this.provider=provider;this.brevoKey=brevoKey;this.mapper=mapper;
        if(!Set.of("none","smtp","brevo").contains(provider))throw new IllegalArgumentException("MAIL_PROVIDER must be none, smtp, or brevo");
        URI uri=URI.create(this.base);
        if(!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
            throw new IllegalArgumentException("Configure PUBLIC_BASE_URL as a trusted http/https application URL");
    }
    public boolean configured(){return !from.isBlank() && (provider.equals("brevo")?!brevoKey.isBlank():provider.equals("smtp")&&!host.isBlank()&&sender.getIfAvailable()!=null);}
    public String provider(){return provider;}
    public void send(String to,String subject,String body) {
        if(!configured())throw new IllegalStateException("Email delivery is not configured");
        if(provider.equals("brevo")) {
            try {
                var address=new jakarta.mail.internet.InternetAddress(from,true);
                String payload=mapper.writeValueAsString(Map.of("sender",Map.of("name","TrackEase","email",address.getAddress()),"to",List.of(Map.of("email",to)),"subject",subject,"textContent",body));
                HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("Accept","application/json").header("api-key",brevoKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
                var response=http.send(request,HttpResponse.BodyHandlers.discarding());
                if(response.statusCode()<200||response.statusCode()>299)throw new IllegalStateException("Email provider rejected delivery");
                return;
            }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Email delivery interrupted");}
            catch(java.io.IOException|jakarta.mail.internet.AddressException e){throw new IllegalStateException("Email delivery failed");}
        }
        SimpleMailMessage mail=new SimpleMailMessage();mail.setFrom(from);mail.setTo(to);mail.setSubject(subject);mail.setText(body);sender.getObject().send(mail);
    }
    public void reset(String recipient,String token) {
        send(recipient,"TrackEase: reset your password","A password reset was requested for your TrackEase account.\n\n"+base+"/reset-password.jsp#token="+token
            +"\n\nThis single-use link expires in 30 minutes. If you did not request it, ignore this message. Your password has not changed.");
    }
    @Scheduled(fixedDelayString="${app.mail.poll-delay:15000}",initialDelay=15000)
    public void deliverPending() {
        if(!configured())return;
        for(int i=0;i<10;i++) {
            try(Connection c=DBUtil.getConnection()) {
                c.setAutoCommit(false);
                try {
                    long id;int ticketId;String email,event;
                    try(PreparedStatement s=c.prepareStatement("SELECT n.*,u.email FROM notification_outbox n JOIN users u ON u.user_id=n.user_id WHERE n.state='PENDING' AND n.next_attempt_at<=CURRENT_TIMESTAMP AND n.created_at>CURRENT_TIMESTAMP-INTERVAL '1 day' ORDER BY n.notification_id LIMIT 1 FOR UPDATE OF n SKIP LOCKED");ResultSet r=s.executeQuery()) {
                        if(!r.next()){c.rollback();return;}id=r.getLong("notification_id");ticketId=r.getInt("ticket_id");email=r.getString("email");event=r.getString("event_type");
                    }
                    try {
                        Ticket t=new TicketDAO().getTicketById(ticketId);
                        if(t==null)throw new IllegalStateException("Ticket unavailable");
                        send(email,"TrackEase ticket #"+ticketId+": "+event.replace('_',' '),
                            "Reservation event: "+event+"\nPassenger: "+(t.getPassengerName()!=null?t.getPassengerName():t.getUser().getFullName())
                            +"\nTrain: "+t.getJourney().getTrain().getTrainName()+"\nCurrent ticket status: "+t.getStatus()
                            +"\nJourney: "+t.getJourney().getDepartureStation().getStationName()+" to "+t.getJourney().getArrivalStation().getStationName()
                            +"\nDeparture: "+t.getJourney().getDepartureTime()+"\n\nView your latest ticket: "+base+"/user/ticketConfirmation.jsp?ticketId="+ticketId
                            +"\n\nTrackEase portfolio demonstration. No payment was collected. This is not an official travel document.");
                        try(PreparedStatement s=c.prepareStatement("UPDATE notification_outbox SET state='SENT',sent_at=NOW(),attempts=attempts+1 WHERE notification_id=?")){s.setLong(1,id);s.executeUpdate();}
                    }catch(RuntimeException e) {
                        try(PreparedStatement s=c.prepareStatement("UPDATE notification_outbox SET attempts=attempts+1,state=CASE WHEN attempts>=4 THEN 'FAILED' ELSE 'PENDING' END,next_attempt_at=CURRENT_TIMESTAMP+INTERVAL '5 minutes' WHERE notification_id=?")){s.setLong(1,id);s.executeUpdate();}
                        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Email delivery attempt failed for notification {}",id);
                    }
                    c.commit();
                }catch(SQLException | RuntimeException e){c.rollback();throw e;}
            }catch(SQLException | RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Email notification processing is currently unavailable");return;}
        }
    }
}
