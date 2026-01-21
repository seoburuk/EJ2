package com.ej2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DaySchedule {
    @JsonProperty("day")
    private Integer day;        // 1=月, 2=火, 3=水, 4=木, 5=金

    @JsonProperty("periodStart")
    private Integer periodStart; // 1-7

    @JsonProperty("periodEnd")
    private Integer periodEnd;   // 1-7

    public DaySchedule() {
    }

    public DaySchedule(Integer day, Integer periodStart, Integer periodEnd) {
        this.day = day;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and Setters
    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Integer periodStart) {
        this.periodStart = periodStart;
    }

    public Integer getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Integer periodEnd) {
        this.periodEnd = periodEnd;
    }
}
