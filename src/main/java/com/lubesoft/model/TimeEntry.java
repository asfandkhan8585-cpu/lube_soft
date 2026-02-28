package com.lubesoft.model;

public class TimeEntry {
    private int id;
    private int userId;
    private String clockIn;
    private String clockOut;
    private String notes;

    public TimeEntry() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getClockIn() { return clockIn; }
    public void setClockIn(String clockIn) { this.clockIn = clockIn; }

    public String getClockOut() { return clockOut; }
    public void setClockOut(String clockOut) { this.clockOut = clockOut; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return clockOut == null || clockOut.isEmpty(); }
}
