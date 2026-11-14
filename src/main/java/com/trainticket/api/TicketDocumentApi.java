package com.trainticket.api;

import com.trainticket.dao.TicketDAO;
import com.trainticket.model.Ticket;
import com.trainticket.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/bookings")
public class TicketDocumentApi {
    private final ReservationService reservations;
    public TicketDocumentApi(ReservationService reservations){this.reservations=reservations;}
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> ticket(@PathVariable int id,HttpServletRequest request) throws IOException {
        Ticket ticket=reservations.ownTicket(AuthApi.user(request.getSession(false)).getUserId(),id);
        return document(List.of(ticket),"ticket-"+id);
    }
    @GetMapping(value="/group/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> group(@PathVariable UUID id,HttpServletRequest request) throws IOException {
        List<Ticket> tickets=new TicketDAO().getBookingGroup(AuthApi.user(request.getSession(false)).getUserId(),id.toString());
        if(tickets.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Booking not found");
        return document(tickets,"booking-"+id);
    }
    static ResponseEntity<byte[]> document(List<Ticket> tickets,String filename) throws IOException {
        try(PDDocument pdf=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            PDFont regular;
            try(InputStream font=PDDocument.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")){
                regular=PDType0Font.load(pdf,Objects.requireNonNull(font,"PDF font unavailable"));
            }
            PDFont bold=regular;
            for(Ticket ticket:tickets) {
                PDPage page=new PDPage(PDRectangle.A4);pdf.addPage(page);
                try(PDPageContentStream canvas=new PDPageContentStream(pdf,page)) {
                    canvas.setNonStrokingColor(34/255f,91/255f,70/255f);canvas.addRect(0,720,595,122);canvas.fill();
                    line(canvas,bold,27,45,787,"TrackEase / Passenger ticket",true);
                    line(canvas,regular,12,45,755,"Ticket #"+ticket.getTicketId()+"  |  "+ticket.getStatus(),true);
                    int y=680;
                    String passenger=ticket.getPassengerName()!=null?ticket.getPassengerName():ticket.getUser().getFullName();
                    String[] rows={"Passenger: "+passenger,"Train: "+ticket.getJourney().getTrain().getTrainNumber()+" - "+ticket.getJourney().getTrain().getTrainName(),
                        "From: "+ticket.getJourney().getDepartureStation().getStationName(),"Departure: "+ticket.getJourney().getDepartureTime(),
                        "To: "+ticket.getJourney().getArrivalStation().getStationName(),"Arrival: "+ticket.getJourney().getArrivalTime(),
                        "Class: "+ticket.getTravelClass(),"Status: "+ticket.getStatus(),"Allocation: "+("WAITLISTED".equals(ticket.getStatus())?"Waiting position "+ticket.getWaitingPosition():("RAC".equals(ticket.getStatus())?"Shared RAC berth ":"Seat ")+ticket.getSeatNumber()+" / Coach "+ticket.getCoachNumber()),
                        "Recorded fare: INR "+String.format(Locale.ENGLISH,"%,.2f",ticket.getJourney().getFare()),"Booked on: "+ticket.getBookingDate(),
                        "Booking reference: "+Objects.toString(ticket.getBookingGroupId(),"Individual reservation")};
                    for(String row:rows) {
                        for(String part:wrap(row,74)){line(canvas,regular,11,45,y,part,false);y-=19;}
                        y-=12;
                    }
                    line(canvas,bold,10,45,105,"PORTFOLIO DEMONSTRATION - NOT AN OFFICIAL TRAVEL DOCUMENT",false);
                    line(canvas,regular,10,45,85,"No payment collected. Check the web ticket for the latest booking status.",false);
                    line(canvas,regular,9,45,65,"Characters unsupported by the PDF font are shown as [?]; original names remain on the web ticket.",false);
                }
            }
            pdf.getDocumentInformation().setTitle("TrackEase reservation");pdf.save(output);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.APPLICATION_PDF).body(output.toByteArray());
        }
    }
    private static List<String> wrap(String value,int length) {
        List<String> rows=new ArrayList<>();for(int start=0;start<value.length();start+=length) rows.add(value.substring(start,Math.min(start+length,value.length())));return rows;
    }
    private static void line(PDPageContentStream out,PDFont font,int size,int x,int y,String value,boolean white) throws IOException {
        StringBuilder safe=new StringBuilder();
        for(int cp:value.codePoints().toArray()) {
            String s=new String(Character.toChars(cp));
            try{font.encode(s);safe.append(s);}catch(IllegalArgumentException e){safe.append("[?]");}
        }
        if(white)out.setNonStrokingColor(1f,1f,1f);else out.setNonStrokingColor(.1f,.23f,.2f);
        out.beginText();out.setFont(font,size);out.newLineAtOffset(x,y);out.showText(safe.toString());out.endText();
    }
}
