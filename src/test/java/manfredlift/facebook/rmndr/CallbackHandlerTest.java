package manfredlift.facebook.rmndr;

import io.dropwizard.jersey.setup.JerseyEnvironment;
import manfredlift.facebook.rmndr.api.*;
import manfredlift.facebook.rmndr.client.FbClient;
import manfredlift.facebook.rmndr.client.WitClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Scheduler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CallbackHandlerTest {
    @Mock FbClient fbClient;
    @Mock WitClient witClient;
    @Mock Scheduler scheduler;

    private CallbackHandler callbackHandler;

    @Before
    public void setup() {
//        RmndrConfiguration rmndrConfiguration = new RmndrConfiguration();
//        rmndrConfiguration.setPageAccessToken("test_page_access_token");
//        rmndrConfiguration.setWitToken("test_wit_token");

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

}
