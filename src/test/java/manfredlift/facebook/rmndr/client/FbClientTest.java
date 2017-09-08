package manfredlift.facebook.rmndr.client;

import com.google.common.collect.ImmutableList;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.OutboundRequest;
import manfredlift.facebook.rmndr.api.QuickReply;
import manfredlift.facebook.rmndr.api.UserTimezone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class FbClientTest {
    private FbClient fbClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Client client;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebTarget target;

    @Before
    public void setup() {
        RmndrConfiguration config = new RmndrConfiguration();
        config.setPageAccessToken("some_access_token");

        fbClient = new FbClient(config, client);
    }

    @Test
    public void sendTextMessageTest() throws ExecutionException, InterruptedException {
        ArgumentCaptor<Entity> argumentCaptor = ArgumentCaptor.forClass(Entity.class);
        when(client.target(RmndrConstants.MESSAGES_URI).queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token"))
            .thenReturn(target);
        when(target.request(MediaType.APPLICATION_JSON).post(any())).thenReturn(Response.ok().build());

        fbClient.sendTextMessage("some_id", "some_text").get();

        verify(client.target(RmndrConstants.MESSAGES_URI)).queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token");
        verify(target.request(MediaType.APPLICATION_JSON)).post(argumentCaptor.capture());
        Entity<OutboundRequest> value = argumentCaptor.getValue();
        OutboundRequest entity = value.getEntity();

        assertEquals("some_text", entity.getMessage().getText());
        assertEquals("some_id", entity.getRecipient().getId());
    }

    @Test
    public void sendErrorMessageTest() throws ExecutionException, InterruptedException {
        ArgumentCaptor<Entity> argumentCaptor = ArgumentCaptor.forClass(Entity.class);
        when(client.target(RmndrConstants.MESSAGES_URI).queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token"))
            .thenReturn(target);
        when(target.request(MediaType.APPLICATION_JSON).post(any())).thenReturn(Response.ok().build());

        fbClient.sendErrorMessage("some_id", "some_error").get();

        verify(client.target(RmndrConstants.MESSAGES_URI)).queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token");
        verify(target.request(MediaType.APPLICATION_JSON)).post(argumentCaptor.capture());
        Entity<OutboundRequest> value = argumentCaptor.getValue();
        OutboundRequest entity = value.getEntity();

        assertEquals("some_error", entity.getMessage().getText());
        assertEquals("some_id", entity.getRecipient().getId());
    }

    @Test
    public void sendQuickReply() throws ExecutionException, InterruptedException {
        ArgumentCaptor<Entity> argumentCaptor = ArgumentCaptor.forClass(Entity.class);
        when(client.target(RmndrConstants.MESSAGES_URI).queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token"))
            .thenReturn(target);
        when(target.request(MediaType.APPLICATION_JSON).post(any())).thenReturn(Response.ok().build());

        QuickReply quickReplyToSend = QuickReply.builder().title("some_title").payload("some_payload").build();
        fbClient.sendQuickReply("some_id", "some_text", ImmutableList.of(quickReplyToSend)).get();

        verify(client.target(RmndrConstants.MESSAGES_URI)).queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token");
        verify(target.request(MediaType.APPLICATION_JSON)).post(argumentCaptor.capture());
        Entity<OutboundRequest> value = argumentCaptor.getValue();
        OutboundRequest entity = value.getEntity();

        assertEquals("some_text", entity.getMessage().getText());
        assertEquals("some_id", entity.getRecipient().getId());
        assertTrue(entity.getMessage().getQuickReplies().stream().findFirst().isPresent());
        assertEquals(quickReplyToSend, entity.getMessage().getQuickReplies().stream().findFirst().orElse(null));
    }

    @Test
    public void getUserTimezoneTest() throws ExecutionException, InterruptedException {
        String userId = "some_id";

        when(client.target(RmndrConstants.BASE_URI)
            .path(userId)
            .queryParam(RmndrConstants.FIELDS, "timezone")
            .queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token"))
        .thenReturn(target);

        when(target.request().get(UserTimezone.class)).thenReturn(new UserTimezone(3));

        CompletableFuture<UserTimezone> userTimezoneFuture = fbClient.getUserTimezoneFuture(userId);

        verify(client.target(RmndrConstants.BASE_URI).path(userId).queryParam(RmndrConstants.FIELDS, "timezone"))
            .queryParam(RmndrConstants.ACCESS_TOKEN, "some_access_token");
        verify(target.request()).get(UserTimezone.class);

        assertEquals(3, userTimezoneFuture.get().getOffsetHours());
    }


}
