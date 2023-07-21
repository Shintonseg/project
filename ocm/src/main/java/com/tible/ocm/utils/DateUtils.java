package com.tible.ocm.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static String fillBasicIsoDate(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.BASIC_ISO_DATE) + "000000";
    }

}
