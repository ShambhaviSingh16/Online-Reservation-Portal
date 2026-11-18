package com.trainticket.persistence.entity;

import jakarta.persistence.*;

/** Persistence model kept separate from the existing browser/API Train DTO. */
@Entity
@Table(name = "trains")
public class TrainEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "train_id")
    private Integer id;
    @Column(name = "train_number", nullable = false, unique = true, length = 30)
    private String number;
    @Column(name = "train_name", nullable = false, length = 200)
    private String name;
    @Column(name = "total_seats", nullable = false)
    private int totalSeats;
    @Column(name = "created_by")
    private Integer createdBy;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Integer getId() { return id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
