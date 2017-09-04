package manfredlift.facebook.rmndr;

public class RmndrMessageConstants {
    public static final String REMINDER_HELP = "To schedule a reminder:\n'!reminder <reminder time>; <reminder text>'";
    public static final String UNPARSABLE_DATE = "Could not parse the date. Please try again.";
    public static final String DATE_MUST_BE_IN_FUTURE = "Date for the timer has to be in the future.";
    public static final String SCHEDULING_ERROR = "Error when scheduling the reminder. Please try again.";
    public static final String UNEXPECTED_ERROR_PLEASE_TRY_AGAIN = "Unexpected error. Please try again.";
    public static final String TIMER_SCHEDULED_SUCCESSFULLY = "Reminder scheduled successfully.";
    public static final String USER_CONFIRMATION = "Set reminder '%s' for '%s'?";
    public static final String NO_REMINDERS_SCHEDULED = "No reminders scheduled.";
    public static final String LIST_REMINDER_ENTRY = "id: %s\ntext: %s\ndate: %s";
    public static final String CANCEL_REMINDER_HELP = "To cancel a reminder:\n" +
        "'!cancel <reminder_id>'";
    public static final String SUCCESSFULLY_CANCELLED_REMINDER = "Successfully cancelled the reminder.";
    public static final String COULD_NOT_CANCEL_REMINDER = "Could not cancel reminder with that id.";
    public static final String SUCCESSFULLY_CLEARED_REMINDERS = "Successfully cleared all reminders.";
    public static final String COULD_NOT_CLEAR_REMINDERS = "Could not clear all reminders.";

    public static final String HELP_MESSAGE = "Could not parse the message. " +
        REMINDER_HELP + "\n" +
        "To list all reminders:\n'!list'\n" +
        CANCEL_REMINDER_HELP + "\n" +
        "To clear all reminders:\n'!clear";

}
