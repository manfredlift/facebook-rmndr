package manfredlift.facebook.rmndr.resources;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.ReminderJob;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.RmndrMessageConstants;
import manfredlift.facebook.rmndr.api.*;
import manfredlift.facebook.rmndr.client.FbClient;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
@Path("/webhook")
@Produces(MediaType.APPLICATION_JSON)
public class WebhookResource {
    private final RmndrConfiguration config;
    private final FbClient fbClient;
    private final Scheduler scheduler;
    private final Gson gson;

    public WebhookResource(RmndrConfiguration config, FbClient fbClient, Scheduler scheduler) {
        this.config = config;
        this.fbClient = fbClient;
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
        processCallbackAsync(callback);
        return Response.ok().build();
    }


    private void processCallbackAsync(Callback callback) {
        CompletableFuture.runAsync(() -> {
            callback.getEntry().forEach(entry -> entry.getMessaging().forEach(this::processMessage));
        });
    }


    private void processMessage(Messaging messaging) {
        Optional.ofNullable(messaging)
            .map(Messaging::getMessage)
            .map(message -> {
                if (message.getQuickReply() != null) {
                    log.debug("Quick reply received");
                    processQuickReply(messaging.getSender(), message.getQuickReply());
                    return message;
                } else {
                    return Optional.ofNullable(message.getNlp())
                        .map(Nlp::getEntities)
                        .map(entities -> {
                            log.info("Message with NLP received");
                            processNlpMessage(messaging.getSender(), entities, message.getText());
                            return message;
                        })
                        .orElse(null);
                }
            })
            .orElseGet(() -> {
                log.error("One of the required fields is null in incoming message: {}", messaging.toString());
                fbClient.sendErrorMessage(messaging.getSender().getId(), RmndrMessageConstants.UNPARSABLE_MESSAGE);
                 return null;
            });
    }


    private void processNlpMessage(User user, Map<String, List<NlpEntity>> entities, String text) {
        List<NlpEntity> reminderEntityList = entities.get("reminder");
        List<NlpEntity> datetimeEntityList = entities.get("datetime");

        if (reminderEntityList == null || datetimeEntityList == null || text == null || !text.startsWith("!reminder")) {
            log.info("Date or reminder entities not present in NLP map");
            fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_MESSAGE);
            return;
        }

        NlpEntity reminderEntity = reminderEntityList.stream().findFirst().orElse(null);
        NlpEntity dateTimeEntity = datetimeEntityList.stream().findFirst().orElse(null);

        if (reminderEntity != null && dateTimeEntity != null) {
            createConfirmationQuickReply(user, reminderEntity, dateTimeEntity);
        } else {
            log.warn("Date or reminder fields empty in NLP entity list");
            fbClient.sendErrorMessage(user.getId(), RmndrMessageConstants.UNPARSABLE_MESSAGE);
        }
    }

    private void createConfirmationQuickReply(User user, NlpEntity reminderEntity, NlpEntity dateTimeEntity) {
        String confirmationText = String.format(RmndrMessageConstants.USER_CONFIRMATION,
            reminderEntity.getValue(), dateTimeEntity.getValue());

        ReminderPayload reminderPayload = new ReminderPayload();
        reminderPayload.setText(reminderEntity.getValue());
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
            log.error("Error when scheduling a job: {}:{}", e.getClass().getCanonicalName(), e.getMessage());
            fbClient.sendErrorMessage(userId, RmndrMessageConstants.SCHEDULING_ERROR);
        }
    }
}
