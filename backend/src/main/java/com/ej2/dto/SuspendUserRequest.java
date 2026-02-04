package com.ej2.dto;

public class SuspendUserRequest {
    private String duration;  // "1_DAY", "3_DAYS", "7_DAYS", "30_DAYS", "PERMANENT"
    private String reason;

    // Constructors
    public SuspendUserRequest() {
    }

    // Getters and Setters
    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
