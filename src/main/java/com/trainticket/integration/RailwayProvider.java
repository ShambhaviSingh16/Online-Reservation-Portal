package com.trainticket.integration;

import java.time.LocalDate;
import java.util.List;

/** Implement only against an authorized provider contract; no scraping or fabricated live results. */
public interface RailwayProvider {
    record Capabilities(String name,boolean configured,boolean liveTimetables,boolean liveReservations,String message){}
    record Timetable(String providerReference,String trainNumber,String trainName,String origin,String destination,String departure,String arrival){}
    Capabilities capabilities();
    List<Timetable> search(String departureCode,String arrivalCode,LocalDate date);
}
