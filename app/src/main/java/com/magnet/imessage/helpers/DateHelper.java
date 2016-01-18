package com.magnet.imessage.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    private static final String DATE_TEMPLATE_WITHOUT_SPACE = "yyyyMMddHHmmss";

    public static String getDateString(Date date) {
        if (date == null) {
            return "";
        }
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        if (dateFormat.format(date).equals(dateFormat.format(new Date()))) {
            return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        } else {
            long week = 1000 * 60 * 60 * 24 * 7;
            if ((System.currentTimeMillis() - date.getTime()) < week) {
                return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
            }
        }
        return dateFormat.format(date);
    }

    public static String getDateWithoutSpaces() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TEMPLATE_WITHOUT_SPACE);
        return dateFormat.format(new Date());
    }

}
