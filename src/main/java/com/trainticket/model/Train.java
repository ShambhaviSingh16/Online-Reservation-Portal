package com.trainticket.model;

public class Train {
    private int trainId;
    private String trainNumber;
    private String trainName;
    private int totalSeats;
    
    // Constructors, getters, and setters
    public Train() {}
    
    public Train(int trainId, String trainNumber, String trainName, int totalSeats) {
        this.trainId = trainId;
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.totalSeats = totalSeats;
    }
    
    // Getters and Setters
    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
}