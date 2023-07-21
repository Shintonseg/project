package com.tible.ocm.exceptions;

import java.time.LocalDateTime;

public class DateInFutureException extends ImportException {

    private LocalDateTime dateTime;

    private LocalDateTime dateTimeNow;

    public DateInFutureException(String message, LocalDateTime dateTime, LocalDateTime dateTimeNow) {
        super(message);
        this.dateTime = dateTime;
        this.dateTimeNow = dateTimeNow;
    }

    public DateInFutureException() {
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public LocalDateTime getDateTimeNow() {
        return dateTimeNow;
    }

    public void setDateTimeNow(LocalDateTime dateTimeNow) {
        this.dateTimeNow = dateTimeNow;
    }
}
