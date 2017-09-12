package manfredlift.facebook.rmndr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import manfredlift.facebook.rmndr.api.*;
import manfredlift.facebook.rmndr.client.FbClient;
import manfredlift.facebook.rmndr.client.WitClient;
import manfredlift.facebook.rmndr.util.DateHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;
import static org.quartz.JobBuilder.newJob;

@RunWith(MockitoJUnitRunner.class)
public class CallbackHandlerTest {
    @Mock FbClient fbClient;
    @Mock WitClient witClient;
    @Mock Scheduler scheduler;

    private CallbackHandler callbackHandler;

    @Before
    public void setup() {
        JerseyEnvironment jersey = mock(JerseyEnvironment.class);

        when(jersey.getProperty(RmndrConstants.FB_CLIENT)).thenReturn(fbClient);
        when(jersey.getProperty(RmndrConstants.WIT_CLIENT)).thenReturn(witClient);
        when(jersey.getProperty(RmndrConstants.QUARTZ_SCHEDULER)).thenReturn(scheduler);

        callbackHandler = new CallbackHandler(jersey);
    }

    @Test
    public void entryMissingTest() throws ExecutionException, InterruptedException {
        Callback callback = Callback.builder().object("page").entry(null).build();

        callbackHandler.handleCallbackAsync(callback).get();

        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void messagingMissingTest() throws ExecutionException, InterruptedException {
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(null).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void messageMissingTest() throws ExecutionException, InterruptedException {
        Messaging messaging = Messaging.builder().sender(new User("some_id")).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void getStartedPostbackTest_success() throws ExecutionException, InterruptedException {
        Postback postback = Postback.builder().title("any title").payload(RmndrConstants.GET_STARTED).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).postback(postback).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verify(fbClient).sendTextMessage("some_id", RmndrMessageConstants.GET_STARTED);

        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void getStartedPostbackTest_senderMissing() throws ExecutionException, InterruptedException {
        Postback postback = Postback.builder().title("any title").payload(RmndrConstants.GET_STARTED).build();
        Messaging messaging = Messaging.builder().postback(postback).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void invalidPostbackTest() throws ExecutionException, InterruptedException {
        Postback postback = Postback.builder().title("any title").payload("invalid payload").build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).postback(postback).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void receiveYesQuickReplyTest_success() throws Exception {
        String payload = "{\"text\":\"some_text\",\"date\":\"2050-09-11T12:30:00.000+01:00\"}";
        QuickReply quickReply = QuickReply.builder().title("Yes").payload(payload).build();
        Message message = Message.builder().mid("some_mid").quickReply(quickReply).seq(1).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).message(message).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();

        ArgumentCaptor<JobDetail> jobDetailArgumentCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerArgumentCaptor = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).scheduleJob(jobDetailArgumentCaptor.capture(), triggerArgumentCaptor.capture());
        verify(fbClient).sendTextMessage("some_id", RmndrMessageConstants.TIMER_SCHEDULED_SUCCESSFULLY);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);

        JobDetail jobDetail = jobDetailArgumentCaptor.getValue();
        Trigger trigger = triggerArgumentCaptor.getValue();

        assertThat(jobDetail.getJobDataMap().get("recipient"), equalTo("some_id"));
        assertThat(jobDetail.getJobDataMap().get("text"), equalTo("some_text"));
        assertThat(jobDetail.getJobDataMap().get("date"), equalTo("2050-09-11T12:30:00.000+01:00"));
        assertThat(trigger.getStartTime().toInstant().toEpochMilli(), equalTo(2546508600000L));
    }

    @Test
    public void receiveYesQuickReplyTest_oldDateFail() throws Exception {
        String payload = "{\"text\":\"some_text\",\"date\":\"2000-09-11T12:30:00.000+01:00\"}";
        QuickReply quickReply = QuickReply.builder().title("Yes").payload(payload).build();
        Message message = Message.builder().mid("some_mid").quickReply(quickReply).seq(1).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).message(message).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();

        verify(fbClient).sendErrorMessage("some_id", RmndrMessageConstants.DATE_MUST_BE_IN_FUTURE);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void receiveYesQuickReplyTest_invalidDateFail() throws Exception {
        String payload = "{\"text\":\"some_text\",\"date\":\"2sdadas2213120\"}";
        QuickReply quickReply = QuickReply.builder().title("Yes").payload(payload).build();
        Message message = Message.builder().mid("some_mid").quickReply(quickReply).seq(1).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).message(message).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        try {
            callbackHandler.handleCallbackAsync(callback).get();
            throw new Exception("Should have failed to parse the date");
        } catch (ExecutionException executionException) {
            verifyZeroInteractions(fbClient, witClient, scheduler);
            Throwable cause = executionException.getCause();
            assertThat(cause.getClass(), equalTo(DateTimeParseException.class));
        }
    }

    @Test
    public void receiveYesQuickReplyTest_invalidJsonPayload() throws Exception {
        String payload = "{\"asd\":\"some_text\",\"jdsajkads\":\"dskalasdklas\"}";
        QuickReply quickReply = QuickReply.builder().title("Yes").payload(payload).build();
        Message message = Message.builder().mid("some_mid").quickReply(quickReply).seq(1).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).message(message).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void receiveCancelQuickReplyTest() throws ExecutionException, InterruptedException {
        QuickReply quickReply = QuickReply.builder().title("Cancel").payload(RmndrConstants.CANCEL).build();
        Message message = Message.builder().mid("some_mid").quickReply(quickReply).seq(1).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).message(message).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);
        Callback callback = Callback.builder().object("page").entry(entries).build();

        callbackHandler.handleCallbackAsync(callback).get();
        verifyZeroInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_unknown() throws ExecutionException, InterruptedException {
        Callback callback = createProcessMessagePayload("help me please");

        callbackHandler.handleCallbackAsync(callback).get();

        verify(fbClient).sendErrorMessage("some_id", RmndrMessageConstants.HELP_MESSAGE);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_invalidReminderCommand() throws ExecutionException, InterruptedException {
        Callback callback = createProcessMessagePayload("!reminder at 11 yoyoyo");
        callbackHandler.handleCallbackAsync(callback).get();

        Callback callback2 = createProcessMessagePayload("!reminder at 11;");
        callbackHandler.handleCallbackAsync(callback2).get();

        Callback callback3 = createProcessMessagePayload("!reminder");
        callbackHandler.handleCallbackAsync(callback3).get();

        verify(fbClient, times(3)).sendErrorMessage("some_id", RmndrMessageConstants.REMINDER_HELP);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_successfulReminderCommand() throws Exception {
        Callback callback = createProcessMessagePayload("!reminder in 5 secs; yassss");

        String witResponseString = "{\"msg_id\":\"03zff2HjZnMuFXtxw\",\"_text\":\"in 5 secs; yassss\",\"entities\":{\"datetime\":[{\"confidence\":0.968155,\"values\":[{\"value\":\"2014-10-30T12:18:50.000+07:00\",\"grain\":\"second\",\"type\":\"value\"}],\"value\":\"2014-10-30T12:18:50.000+07:00\",\"grain\":\"second\",\"type\":\"value\"}]}}";

        when(fbClient.getUserTimezoneFuture("some_id")).thenReturn(CompletableFuture.completedFuture(new UserTimezone(1)));
        when(witClient.getResponseFuture("in 5 secs", DateHelper.referenceTimeFromMillis(1503652953801L, 1)))
            .thenReturn(CompletableFuture.completedFuture(new ObjectMapper().readValue(witResponseString, WitResponse.class)));

        callbackHandler.handleCallbackAsync(callback).get();

        verify(fbClient).getUserTimezoneFuture("some_id");
        verify(witClient).getResponseFuture("in 5 secs", DateHelper.referenceTimeFromMillis(1503652953801L, 1));
        verify(fbClient).sendQuickReply(eq("some_id"), anyString(), anyList());
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_failedReminderCommand_failedToGetTimezone() throws Exception {
        Callback callback = createProcessMessagePayload("!reminder in 5 secs; yassss");

        CompletableFuture<UserTimezone> userTimezoneCompletableFuture = new CompletableFuture<>();
        userTimezoneCompletableFuture.completeExceptionally(new RuntimeException());

        when(fbClient.getUserTimezoneFuture("some_id")).thenReturn(userTimezoneCompletableFuture);

        callbackHandler.handleCallbackAsync(callback).get();

        verify(fbClient).getUserTimezoneFuture("some_id");
        verify(fbClient).sendErrorMessage("some_id", RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_failedReminderCommand_failedToGetWitResponse() throws Exception {
        Callback callback = createProcessMessagePayload("!reminder in 5 secs; yassss");

        CompletableFuture<WitResponse> witResponseCompletableFuture = new CompletableFuture<>();
        witResponseCompletableFuture.completeExceptionally(new RuntimeException());

        when(fbClient.getUserTimezoneFuture("some_id")).thenReturn(CompletableFuture.completedFuture(new UserTimezone(1)));
        when(witClient.getResponseFuture("in 5 secs", DateHelper.referenceTimeFromMillis(1503652953801L, 1)))
            .thenReturn(witResponseCompletableFuture);

        callbackHandler.handleCallbackAsync(callback).get();

        verify(fbClient).getUserTimezoneFuture("some_id");
        verify(witClient).getResponseFuture("in 5 secs", DateHelper.referenceTimeFromMillis(1503652953801L, 1));
        verify(fbClient).sendErrorMessage("some_id", RmndrMessageConstants.UNEXPECTED_ERROR_PLEASE_TRY_AGAIN);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_listCommand_noReminders() throws Exception {
        Callback callback = createProcessMessagePayload("!list");

        callbackHandler.handleCallbackAsync(callback).get();

        verify(scheduler).getJobKeys(GroupMatcher.jobGroupEquals("some_id"));
        verify(fbClient).sendTextMessage("some_id", RmndrMessageConstants.NO_REMINDERS_SCHEDULED);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_listCommand_oneReminder() throws Exception {
        Callback callback = createProcessMessagePayload("!list");

        JobDetail job = newJob(ReminderJob.class)
            .withIdentity("12345", "some_id")
            .usingJobData("recipient", "some_id")
            .usingJobData("text", "some_text")
            .usingJobData("date", "some_date")
            .build();

        JobKey jobKey = new JobKey("random_id", "some_id");

        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("some_id")))
            .thenReturn(Collections.singleton(jobKey));
        when(scheduler.getJobDetail(jobKey)).thenReturn(job);

        callbackHandler.handleCallbackAsync(callback).get();

        verify(scheduler).getJobKeys(GroupMatcher.jobGroupEquals("some_id"));
        verify(scheduler).getJobDetail(jobKey);
        verify(fbClient).sendTextMessage("some_id",
            String.format(RmndrMessageConstants.LIST_REMINDER_ENTRY, "random_id", "some_text", "some_date"));
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_cancelCommand_success() throws Exception {
        Callback callback = createProcessMessagePayload("!cancel random_id");

        when(scheduler.deleteJob(new JobKey("random_id", "some_id"))).thenReturn(true);

        callbackHandler.handleCallbackAsync(callback).get();

        verify(scheduler).deleteJob(new JobKey("random_id", "some_id"));
        verify(fbClient).sendTextMessage("some_id", RmndrMessageConstants.SUCCESSFULLY_CANCELLED_REMINDER);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_cancelCommand_notDeleted() throws Exception {
        Callback callback = createProcessMessagePayload("!cancel wrong_id");

        when(scheduler.deleteJob(new JobKey("wrong_id", "some_id"))).thenReturn(false);

        callbackHandler.handleCallbackAsync(callback).get();

        verify(scheduler).deleteJob(new JobKey("wrong_id", "some_id"));
        verify(fbClient).sendErrorMessage("some_id", RmndrMessageConstants.COULD_NOT_CANCEL_REMINDER);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_cancelCommand_noJobIdGiven() throws Exception {
        Callback callback = createProcessMessagePayload("!cancel");

        callbackHandler.handleCallbackAsync(callback).get();

        verify(fbClient).sendErrorMessage("some_id", RmndrMessageConstants.CANCEL_REMINDER_HELP);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    @Test
    public void processMessage_clearCommand_oneReminder() throws Exception {
        Callback callback = createProcessMessagePayload("!clear");

        JobKey jobKey = new JobKey("random_id", "some_id");

        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("some_id")))
            .thenReturn(Collections.singleton(jobKey));
        when(scheduler.deleteJobs(ImmutableList.of(jobKey))).thenReturn(true);

        callbackHandler.handleCallbackAsync(callback).get();

        verify(scheduler).getJobKeys(GroupMatcher.jobGroupEquals("some_id"));
        verify(scheduler).deleteJobs(ImmutableList.of(jobKey));
        verify(fbClient).sendTextMessage("some_id", RmndrMessageConstants.SUCCESSFULLY_CLEARED_REMINDERS);
        verifyNoMoreInteractions(fbClient, witClient, scheduler);
    }

    private Callback createProcessMessagePayload(String text) {
        Message message = Message.builder().mid("some_mid").seq(1).text(text).build();
        Messaging messaging = Messaging.builder().sender(new User("some_id")).message(message).timestamp(1503652953801L).build();
        List<Messaging> messagings = Collections.singletonList(messaging);
        Entry entry = Entry.builder().id(123).time(1503652953801L).messaging(messagings).build();
        List<Entry> entries = Collections.singletonList(entry);

        return Callback.builder().object("page").entry(entries).build();
    }
}
