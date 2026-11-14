package com.trainticket.api;

import com.trainticket.model.*;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketDocumentTest {
    @Test void pdfContainsOnePagePerPassengerAndBookingStatus() throws Exception {
        Ticket t=new Ticket();t.setTicketId(42);t.setPassengerName("Asha Rao");t.setStatus("CONFIRMED");t.setSeatNumber("1");t.setCoachNumber("S1");
        Journey j=new Journey();Train train=new Train();train.setTrainNumber("12001");train.setTrainName("Demo Express");j.setTrain(train);
        Station a=new Station();a.setStationName("New Delhi");Station b=new Station();b.setStationName("Kanpur");j.setDepartureStation(a);j.setArrivalStation(b);
        j.setFare(500);j.setDepartureTime(java.sql.Timestamp.valueOf("2027-01-01 08:00:00"));j.setArrivalTime(java.sql.Timestamp.valueOf("2027-01-01 13:00:00"));t.setJourney(j);
        byte[] bytes=TicketDocumentApi.document(List.of(t,t),"group").getBody();assertNotNull(bytes);
        try(var pdf=Loader.loadPDF(bytes)) {assertEquals(2,pdf.getNumberOfPages());String text=new PDFTextStripper().getText(pdf);assertTrue(text.contains("Asha Rao"));assertTrue(text.contains("CONFIRMED"));assertTrue(text.contains("INR 500.00"));}
    }
}
