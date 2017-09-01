package manfredlift.facebook.rmndr;

public class RmndrConstants {
    public static final String BASE_URI = "https://graph.facebook.com/v2.6";
    public static final String MESSAGES_URI = BASE_URI + "/me/messages";
    public static final String FIELDS = "fields";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String FB_CLIENT = "fb_client";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String CANCEL = "cancel";

    //Wit api
    public static final String WIT_URI = "https://api.wit.ai/message?v=20170901";

    // API commands
    public static final String REMINDER_COMMAND = "!reminder";
    public static final String LIST_COMMAND = "!list";
    public static final String CANCEL_COMMAND = "!cancel";
    public static final String CLEAR_COMMAND = "!clear";
}
