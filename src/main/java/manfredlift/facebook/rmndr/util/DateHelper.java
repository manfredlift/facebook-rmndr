package manfredlift.facebook.rmndr.util;

import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.ReferenceTime;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateHelper {
    public static ReferenceTime referenceTimeFromMillis(long millis, int offsetHours) {
        ZoneId zoneId = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(offsetHours));
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);
        String formattedDate = zonedDateTime.format(DateTimeFormatter.ofPattern(RmndrConstants.DATE_FORMAT));

        return new ReferenceTime(formattedDate);
    }
}
