package com.trainticket.model;

import java.sql.Timestamp;

public class Journey {
    private int journeyId;
    private int trainId;
    private int departureStationId;
    private int arrivalStationId;
    private Timestamp departureTime;
    private Timestamp arrivalTime;
    private double fare;
    private int availableSeats;
    private Train train;
    private Station departureStation;
    private Station arrivalStation;
    
    // Constructors, getters, and setters
    public Journey() {}
    
    // Getters and Setters for all properties
    public int getJourneyId() { return journeyId; }
    public void setJourneyId(int journeyId) { this.journeyId = journeyId; }
    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }
    public int getDepartureStationId() { return departureStationId; }
    public void setDepartureStationId(int departureStationId) { this.departureStationId = departureStationId; }
    public int getArrivalStationId() { return arrivalStationId; }
    public void setArrivalStationId(int arrivalStationId) { this.arrivalStationId = arrivalStationId; }
    public Timestamp getDepartureTime() { return departureTime; }
    public void setDepartureTime(Timestamp departureTime) { this.departureTime = departureTime; }
    public Timestamp getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(Timestamp arrivalTime) { this.arrivalTime = arrivalTime; }
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }
    public Station getDepartureStation() { return departureStation; }
    public void setDepartureStation(Station departureStation) { this.departureStation = departureStation; }
    public Station getArrivalStation() { return arrivalStation; }
    public void setArrivalStation(Station arrivalStation) { this.arrivalStation = arrivalStation; }
}