package manfredlift.facebook.rmndr.resources;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.ReminderJob;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.RmndrMessageConstants;
import manfredlift.facebook.rmndr.api.*;
import manfredlift.facebook.rmndr.client.FbClient;
import manfredlift.facebook.rmndr.client.WitClient;
import manfredlift.facebook.rmndr.util.DateHelper;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static manfredlift.facebook.rmndr.RmndrConstants.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
@Path("/webhook")
@Produces(MediaType.APPLICATION_JSON)
public class WebhookResource {
    private final RmndrConfiguration config;
    private final FbClient fbClient;
    private final WitClient witClient;
    private final Scheduler scheduler;
    private final Gson gson;

    public WebhookResource(RmndrConfiguration config, FbClient fbClient, WitClient witClient, Scheduler scheduler) {
        this.config = config;
        this.fbClient = fbClient;
        this.witClient = witClient;
        this.scheduler = scheduler;
        this.gson = new Gson();
    }

    @GET
    public Response verifyWebhook(@QueryParam("hub.mode") String mode,
                                  @QueryParam("hub.verify_token") String verifyToken,
                                  @QueryParam("hub.challenge") String challenge) {

        if (config.getVerifyToken().equals(verifyToken) && challenge != null) {
            log.info("Verifying webhook successful");
            return Response.ok(challenge).build();
        } else {
            log.warn("Verifying webhook unsuccessful");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }


    @POST
    public Response handleCallback(Callback callback) {
        callback.getEntry().forEach(entry -> entry.getMessaging().forEach(this::processMessaging));
        return Response.ok().build();
    }

    private void processMessaging(Messaging messaging) {
        Message message = messaging.getMessage();
        
        if (message == null) {
            log.error("Unexpected: Message is null in Messaging: {}", messaging.toString());
            return;
        }

        if (message.getQuickReply() != null) {
            log.info("Quick reply received: '{}'", message.getQuickReply().getPayload());
            processQuickReply(messaging.getSender(), message.getQuickReply());
        } else {
            processMessage(messaging.getSender(), message, messaging.getTimestamp());
        }
    }

    private void processMessage(User user, Message message, long timestamp) {
        String text = message.getText();

        if (text == null) {
            log.error("Unexpected: Text is null in Message: {}", message.toString());
            return;
        }

        if (text.startsWith(REMINDER_COMMAND)) {
            if (text.indexOf(';') > 0 && text.indexOf(';') < text.length() - 1) {
                handleReminderCommand(user, text, timestamp);
                return;
            }
        } else if (text.startsWith(LIST_COMMAND)) {
            return;
        } else if (text.startsWith(CANCEL_COMMAND)) {
            return;
        } else if (text.startsWith(CLEAR_COMMAND)) {
            return;
        }

        fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_MESSAGE);
    }


    private void handleReminderCommand(User user, String text, long timestamp) {
        String dateText = text.substring(RmndrConstants.REMINDER_COMMAND.length(), text.indexOf(';')).trim();
        String reminderText = text.substring(text.indexOf(';') + 1).trim();

        fbClient.getUserTimezoneFuture(user.getId())
            .exceptionally(th -> {
                log.error("Error when getting timezone from Facebook. Error: {}:{}", th.getClass().getCanonicalName(),
                    th.getClass().getCanonicalName(), th.getMessage());
                fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
                return null;
            })
            .thenCompose(userTimezone -> {
                String formattedDateString = DateHelper.millisToFormattedDate(timestamp, userTimezone.getTimezone());
                ReferenceTime referenceTime = new ReferenceTime(formattedDateString);
                return witClient.getResponseFuture(dateText, referenceTime);
            })
            .exceptionally(th -> {
                log.error("Error when querying wit.ai. Error: {}:{}", th.getClass().getCanonicalName(),
                    th.getClass().getCanonicalName(), th.getMessage());
                fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
                return null;
            })
            .thenAccept(witResponse -> {
                if (witResponse.getEntities() == null) {
                    log.info("Entity object not present in Wit response. Response: {}", witResponse);
                    fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_DATE);
                }

                List<NlpEntity> datetimeEntityList = witResponse.getEntities().get("datetime");

                if (datetimeEntityList == null) {
                    log.info("Datetime entity not present in NLP map. Response: {}", witResponse);
                    fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_DATE);
                    return;
                }

                NlpEntity dateTimeEntity = datetimeEntityList.stream().findFirst().orElse(null);

                if (dateTimeEntity != null) {
                    createConfirmationQuickReply(user, dateTimeEntity, reminderText);
                } else {
                    log.info("Datetime entity not present datetime entity list. Response: {}", witResponse);
                    fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_DATE);
                }
            });
    }

    private void createConfirmationQuickReply(User user, NlpEntity dateTimeEntity, String reminderText) {
        ZonedDateTime zonedDate = ZonedDateTime.parse(dateTimeEntity.getValue());
        String humanDateString = zonedDate.format(DateTimeFormatter.ofPattern(RmndrConstants.HUMAN_DATE_FORMAT));

        String confirmationText = String.format(RmndrMessageConstants.USER_CONFIRMATION,
            reminderText, humanDateString);

        ReminderPayload reminderPayload = new ReminderPayload();
        reminderPayload.setText(reminderText);
        reminderPayload.setDate(dateTimeEntity.getValue());
        String payload = gson.toJson(reminderPayload);

        QuickReply yesQuickReply = QuickReply.builder().title("Yes").payload(payload).build();
        QuickReply cancelQuickReply = QuickReply.builder().title("Cancel").payload(RmndrConstants.CANCEL).build();

        fbClient.sendQuickReply(user.getId(), confirmationText, ImmutableList.of(yesQuickReply, cancelQuickReply));
        log.info("Quick reply sent");
    }


    private void processQuickReply(User user, QuickReply quickReply) {
        if (quickReply.getPayload() == null || quickReply.getPayload().length() == 0) {
            log.info("User cancelled in quick reply");
            return;
        }

        if (quickReply.getPayload().equals(RmndrConstants.CANCEL)) {
            log.info("User cancelled timer in confirmation");
            return;
        }

        String payload = quickReply.getPayload();
        ReminderPayload reminderPayload = gson.fromJson(payload, ReminderPayload.class);

        Date date;
        try {
            date = new SimpleDateFormat(RmndrConstants.DATE_FORMAT).parse(reminderPayload.getDate());
        } catch (ParseException e) {
            log.warn("Error when parsing the date: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_DATE);
            return;
        }

        if (date.before(new Date())) {
            log.info("Tried to set a reminder in the past");
            fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.DATE_MUST_BE_IN_FUTURE);
            return;
        }

        scheduleReminder(user.getId(), reminderPayload.getText(), date);
    }

    private void scheduleReminder(String userId, String text, Date date) {
        JobDetail job = newJob(ReminderJob.class)
            .withIdentity(UUID.randomUUID().toString(), userId)
            .usingJobData("recipient", userId)
            .usingJobData("text", text)
            .build();

        Trigger trigger = newTrigger()
            .withIdentity(UUID.randomUUID().toString(), userId)
            .startAt(date)
            .build();

        try {
            scheduler.scheduleJob(job, trigger);
            fbClient.sendTextMessage(userId, RmndrMessageConstants.TIMER_SCHEDULED_SUCCESSFULLY);
            log.info("Reminder scheduled");
        } catch (SchedulerException e) {
            log.error("Error when scheduling a job. Error: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(userId, RmndrMessageConstants.SCHEDULING_ERROR);
        }
    }

    private void listReminders(String userId) {
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(userId));
            List<String> jobIds = new ArrayList<>();

            jobKeys.forEach((jobKey) -> {
                jobIds.add(jobKey.getName());
                try {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    JobDataMap jobDataMap = jobDetail.getJobDataMap();
                    jobDataMap.getString(""); // reminder text
                    jobDataMap.getString(""); // reminder date
                } catch (SchedulerException e) {
                    throw new RuntimeException(e);
                }
            });
            for (JobKey jobKey : jobKeys) {
                jobIds.add(jobKey.getName());
            }

        } catch (SchedulerException | RuntimeException e) {
            log.error("Error when listing jobs. Error: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(userId, RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
        }
    }
}
