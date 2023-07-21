package com.tible.ocm.exceptions;

public class WrongVersionNumberException extends ImportException {

    private String number;

    private String expectedNumber;

    public WrongVersionNumberException(String message, String number, String expectedNumber) {
        super(message);
        this.number = number;
        this.expectedNumber = expectedNumber;
    }

    public WrongVersionNumberException() {
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getExpectedNumber() {
        return expectedNumber;
    }

    public void setExpectedNumber(String expectedNumber) {
        this.expectedNumber = expectedNumber;
    }
}
