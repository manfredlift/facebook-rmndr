package manfredlift.facebook.rmndr.client;

import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.*;
import org.apache.http.HttpStatus;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class FbClient {
    private final String accessToken;
    private final Client client;

    public FbClient(RmndrConfiguration configuration, Client client) {
        this.accessToken = configuration.getPageAccessToken();
        this.client = client;
    }

    private void sendMessage(OutboundRequest outboundRequest) {
        CompletableFuture.runAsync(() -> {
            WebTarget target = client.target(RmndrConstants.MESSAGES_URI)
                .queryParam(RmndrConstants.ACCESS_TOKEN, accessToken);

            Entity<OutboundRequest> entity = Entity.entity(outboundRequest, MediaType.APPLICATION_JSON);
            Response response = target.request(MediaType.APPLICATION_JSON).post(entity);

            if (response.getStatus() != HttpStatus.SC_OK) {
                log.error("Could not send message to Facebook. Response: {}", response);
            } else {
                log.info("Message sent to: {}", outboundRequest.getRecipient().getId());
            }
        }).exceptionally(e -> {
            log.error("Unexpected error when sending a message. Error: '{}:{}'",
                e.getClass().getCanonicalName(), e.getMessage());
            return null;
        });
    }

    public void sendTextMessage(String recipientId, String text) {
        OutboundRequest outboundRequest = OutboundRequest.builder()
            .recipient(new User(recipientId))
            .message(OutboundMessage.builder().text(text).build())
            .build();

        sendMessage(outboundRequest);
    }

    public void  sendQuickReply(String recipientId, String text, List<QuickReply> quickReplies) {
        OutboundRequest outboundRequest = OutboundRequest.builder()
            .recipient(new User(recipientId))
            .message(OutboundMessage.builder().text(text).quickReplies(quickReplies).build())
            .build();

        sendMessage(outboundRequest);
    }

    public void sendErrorMessage(String recipientId, String errorMessage) {
        sendTextMessage(recipientId, errorMessage);
    }

    public CompletableFuture<UserTimezone> getUserTimezoneFuture(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            WebTarget target = client.target(RmndrConstants.BASE_URI)
                .path(userId)
                .queryParam(RmndrConstants.FIELDS, "timezone")
                .queryParam(RmndrConstants.ACCESS_TOKEN, accessToken);

            UserTimezone userTimezone = target.request().get(UserTimezone.class);
            log.info("User timezone received: {}", userTimezone.getTimezone());
            return userTimezone;
        }).exceptionally(e -> {
            log.error("Unexpected error when querying user timezone from Facebook. Error: '{}:{}'",
                e.getClass().getCanonicalName(), e.getMessage());
            return null;
        });

    }
}
