package manfredlift.facebook.rmndr.util;

import manfredlift.facebook.rmndr.RmndrConstants;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateHelper {
    public static String millisToFormattedDate(long millis, int offsetHours) {
        String timezoneId = offsetHours >= 0 ? "GMT+" + offsetHours : "GMT" + offsetHours;
        SimpleDateFormat dateFormat = new SimpleDateFormat(RmndrConstants.DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timezoneId));

        return dateFormat.format(millis);
    }
}
