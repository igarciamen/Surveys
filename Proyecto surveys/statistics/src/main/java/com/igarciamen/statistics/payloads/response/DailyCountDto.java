package com.igarciamen.statistics.payloads.response;

import java.time.LocalDate;

public class DailyCountDto {
    private LocalDate date;
    private long count;

    public DailyCountDto(LocalDate date, long count) {
        this.date = date;
        this.count = count;
    }
    public LocalDate getDate() { return date; }
    public long getCount() { return count; }
}
