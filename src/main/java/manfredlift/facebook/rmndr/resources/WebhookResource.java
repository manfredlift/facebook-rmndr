package manfredlift.facebook.rmndr.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import manfredlift.facebook.rmndr.util.SignatureVerifier;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
    private final ObjectMapper objectMapper;

    public WebhookResource(RmndrConfiguration config, FbClient fbClient, WitClient witClient, Scheduler scheduler) {
        this.config = config;
        this.fbClient = fbClient;
        this.witClient = witClient;
        this.scheduler = scheduler;
        this.gson = new Gson();
        this.objectMapper = new ObjectMapper();
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
    public Response handleCallback(@HeaderParam("X-Hub-Signature") String signature,
                                   String requestBody) throws IOException {

        if (SignatureVerifier.isValid(config.getAppSecret(), signature, requestBody)) {
            Callback callback = objectMapper.readValue(requestBody, Callback.class);
            CompletableFuture.runAsync(() ->
                callback.getEntry().forEach(entry -> entry.getMessaging().forEach(this::processMessaging)));

            // acknowledge with response 200 instantly
            return Response.ok().build();
        } else {
            log.warn("Payload verification failed.");
            return  Response.status(Response.Status.FORBIDDEN).build();
        }
    }




    private void processMessaging(Messaging messaging) {
        if (messaging.getPostback() != null) {
            processPostback(messaging.getSender(), messaging.getPostback());
            return;
        }

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

        Date date = Date.from(ZonedDateTime.parse(reminderPayload.getDate()).toInstant());

        if (date.before(new Date())) {
            log.info("Tried to set a reminder in the past");
            fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.DATE_MUST_BE_IN_FUTURE);
            return;
        }

        scheduleReminder(user.getId(), reminderPayload.getText(), reminderPayload.getDate(), date);
    }

    private void processPostback(User user, Postback postback) {
        if (RmndrConstants.GET_STARTED.equals(postback.getPayload())) {
            log.info("Get started postback received");
            fbClient.sendTextMessage(user.getId(), RmndrMessageConstants.GET_STARTED);
        }
    }

    private void processMessage(User user, Message message, long timestamp) {
        String text = message.getText();

        if (text == null) {
            log.info("Unexpected: Text is null in Message: {}", message.toString());
            return;
        }

        if (text.startsWith(REMINDER_COMMAND)) {
            if (text.indexOf(';') > 0 && text.indexOf(';') < text.length() - 1) {
                handleReminderCommand(user, text, timestamp);
            } else {
                fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.REMINDER_HELP);
            }

        } else if (text.startsWith(LIST_COMMAND)) {
            handleListCommand(user.getId());

        } else if (text.startsWith(CANCEL_COMMAND)) {
            String[] splitStrings = text.split("\\s+"); // split string on whitespace
            if (splitStrings.length == 2) {
                handleCancelCommand(user.getId(), splitStrings[1]);
            } else {
                fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.CANCEL_REMINDER_HELP);
            }

        } else if (text.startsWith(CLEAR_COMMAND)) {
            handleClearCommand(user.getId());
        } else {
            fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.HELP_MESSAGE);
        }
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
                ReferenceTime refTime = DateHelper.referenceTimeFromMillis(timestamp, userTimezone.getOffsetHours());
                return witClient.getResponseFuture(dateText, refTime);
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

    private void handleListCommand(String userId) {
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(userId));
            if (jobKeys.size() == 0) {
                fbClient.sendTextMessage(userId, RmndrMessageConstants.NO_REMINDERS_SCHEDULED);
                return;
            }

            for (JobKey jobKey : jobKeys) {
                JobDataMap jobDataMap = scheduler.getJobDetail(jobKey).getJobDataMap();

                String id = jobKey.getName();
                String date = jobDataMap.getString("date");
                String text = jobDataMap.getString("text");
                text = text.length() > 20 ? StringUtils.left(text, 17) + "..." : text;

                fbClient.sendTextMessage(userId, String.format(RmndrMessageConstants.LIST_REMINDER_ENTRY, id, text, date));
            }
        } catch (SchedulerException e) {
            log.error("Error when listing jobs. Error: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(userId, RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
        }
    }

    private void handleCancelCommand(String userId, String reminderId) {
        try {
            JobKey jobKey = new JobKey(reminderId, userId);
            if (scheduler.deleteJob(jobKey)) {
                fbClient.sendTextMessage(userId, RmndrMessageConstants.SUCCESSFULLY_CANCELLED_REMINDER);
            } else {
                fbClient.sendErrorMessage(userId, RmndrMessageConstants.COULD_NOT_CANCEL_REMINDER);
            }

        } catch (SchedulerException e) {
            log.error("Error when cancelling a job. Error: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(userId, RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
        }
    }

    private void handleClearCommand(String userId) {
        try {
            Set<JobKey> jobKeysSet = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(userId));
            List<JobKey> jobKeys = new ArrayList<>(jobKeysSet);
            if (scheduler.deleteJobs(jobKeys)) {
                fbClient.sendTextMessage(userId, RmndrMessageConstants.SUCCESSFULLY_CLEARED_REMINDERS);
            } else {
                fbClient.sendErrorMessage(userId, RmndrMessageConstants.COULD_NOT_CLEAR_REMINDERS);
            }

        } catch (SchedulerException e) {
            log.error("Error when clearing all jobs. Error: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(userId, RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
        }
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

    private void scheduleReminder(String userId, String text, String dateString, Date date) {
        JobDetail job = newJob(ReminderJob.class)
            .withIdentity(UUID.randomUUID().toString().replace("-", ""), userId)
            .usingJobData("recipient", userId)
            .usingJobData("text", text)
            .usingJobData("date", dateString)
            .build();

        Trigger trigger = newTrigger()
            .withIdentity(UUID.randomUUID().toString().replace("-", ""), userId)
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
}