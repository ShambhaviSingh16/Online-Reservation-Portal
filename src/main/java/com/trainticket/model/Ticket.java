package com.trainticket.model;

import java.sql.Timestamp;

public class Ticket {
    private boolean advanced;
    private String travelClass="STANDARD",berthPreference="ANY";
    private int segmentStart,segmentEnd;
    public boolean isAdvanced(){return advanced;}
    public void setAdvanced(boolean value){advanced=value;}
    public String getTravelClass(){return travelClass;}
    public void setTravelClass(String value){travelClass=value;}
    public String getBerthPreference(){return berthPreference;}
    public void setBerthPreference(String value){berthPreference=value;}
    public int getSegmentStart(){return segmentStart;}
    public void setSegmentStart(int value){segmentStart=value;}
    public int getSegmentEnd(){return segmentEnd;}
    public void setSegmentEnd(int value){segmentEnd=value;}
    private String bookingGroupId;
    private String passengerName;
    public String getBookingGroupId() { return bookingGroupId; }
    public void setBookingGroupId(String value) { bookingGroupId=value; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String value) { passengerName=value; }
    private int waitingPosition;
    public int getWaitingPosition() { return waitingPosition; }
    public void setWaitingPosition(int value) { waitingPosition=value; }
    private int ticketId;
    private int journeyId;
    private int userId;
    private Timestamp bookingDate;
    private String seatNumber;
    private String coachNumber;
    private String status;
    private Journey journey;
    private User user;

    // Constructors, getters, and setters
    public Ticket() {}

    // Getters and Setters for all properties
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public int getJourneyId() { return journeyId; }
    public void setJourneyId(int journeyId) { this.journeyId = journeyId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Timestamp getBookingDate() { return bookingDate; }
    public void setBookingDate(Timestamp bookingDate) { this.bookingDate = bookingDate; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public String getCoachNumber() { return coachNumber; }
    public void setCoachNumber(String coachNumber) { this.coachNumber = coachNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Journey getJourney() { return journey; }
    public void setJourney(Journey journey) { this.journey = journey; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
