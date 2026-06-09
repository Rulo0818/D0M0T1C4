package com.domotica.app.model;

public class Device {
    private String name;
    private int ledStatus;
    private long lastUpdated;

    public Device() {}

    public Device(String name, int ledStatus, long lastUpdated) {
        this.name = name;
        this.ledStatus = ledStatus;
        this.lastUpdated = lastUpdated;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLedStatus() { return ledStatus; }
    public void setLedStatus(int ledStatus) { this.ledStatus = ledStatus; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
