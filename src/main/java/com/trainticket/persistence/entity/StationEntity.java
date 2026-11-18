package com.trainticket.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stations")
public class StationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "station_id")
    private Integer id;
    @Column(name = "station_code", nullable = false, unique = true, length = 20)
    private String code;
    @Column(name = "station_name", nullable = false, length = 200)
    private String name;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String state;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Integer getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
