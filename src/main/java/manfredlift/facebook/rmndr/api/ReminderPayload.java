package manfredlift.facebook.rmndr.api;

import lombok.Data;

@Data
public class ReminderPayload {
    private String text;
    private String date;
}
