package com.domotica.app.model;

public class User {
    private String email;
    private String displayName;
    private long createdAt;

    public User() {}

    public User(String email, String displayName, long createdAt) {
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
