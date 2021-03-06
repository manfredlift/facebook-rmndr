package manfredlift.facebook.rmndr;

public class RmndrConstants {
    public static final String BASE_URI = "https://graph.facebook.com/v2.6";
    public static final String MESSAGES_URI = BASE_URI + "/me/messages";
    public static final String FIELDS = "fields";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static final String HUMAN_DATE_FORMAT = "EEE, dd/MMM/yyyy HH:mm:ss z";
    public static final String CANCEL = "cancel";
    public static final String GET_STARTED = "get_started";

    // jersey properties
    public static final String FB_CLIENT = "fb_client";
    public static final String WIT_CLIENT = "wit_client";
    public static final String CALLBACK_HANDLER = "callback_handler";
    public static final String QUARTZ_SCHEDULER = "quartz_scheduler";

    //Wit api
    public static final String WIT_URI = "https://api.wit.ai/message?v=20170901";

    // API commands
    public static final String REMINDER_COMMAND = "!reminder";
    public static final String LIST_COMMAND = "!list";
    public static final String CANCEL_COMMAND = "!cancel";
    public static final String CLEAR_COMMAND = "!clear";
}
