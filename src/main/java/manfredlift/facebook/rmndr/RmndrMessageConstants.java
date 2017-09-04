package manfredlift.facebook.rmndr;

public class RmndrMessageConstants {
    public static final String UNPARSABLE_MESSAGE = "Could not parse the message. " +
        "To set a reminder please format your message like: '!reminder <date and time>; <reminder text>'.\n" +
        "E.g '!reminder tomorrow at 4pm; do laundry'";
    public static final String UNPARSABLE_DATE = "Could not parse the date. Please try again.";
    public static final String DATE_MUST_BE_IN_FUTURE = "Date for the timer has to be in the future.";
    public static final String SCHEDULING_ERROR = "Error when scheduling the reminder. Please try again.";
    public static final String UNEXPECTED_ERROR_PLEASE_TRY_AGAIN = "Unexpected error. Please try again.";
    public static final String TIMER_SCHEDULED_SUCCESSFULLY = "Reminder scheduled successfully.";
    public static final String USER_CONFIRMATION = "Set reminder '%s' for '%s'?";
    public static final String NO_REMINDERS_SCHEDULED = "No reminders scheduled.";
    public static final String LIST_REMINDER_ENTRY = "id: %s\ntext: %s\ndate: %s";
    public static final String CANCEL_REMINDER_HELP = "To cancel a reminder format your message like:\n" +
        "'!cancel <reminder_id>'";
    public static final String SUCCESSFULLY_CANCELLED_REMINDER = "Successfully cancelled the reminder.";
    public static final String COULD_NOT_CANCEL_REMINDER = "Could not cancel reminder with that id.";
}
