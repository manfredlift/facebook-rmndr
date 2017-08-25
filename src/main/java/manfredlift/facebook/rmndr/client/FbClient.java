package manfredlift.facebook.rmndr.client;

import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.OutboundMessage;
import manfredlift.facebook.rmndr.api.OutboundRequest;
import manfredlift.facebook.rmndr.api.QuickReply;
import manfredlift.facebook.rmndr.api.User;

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
        WebTarget target = client.target(RmndrConstants.BASE_URI).queryParam(RmndrConstants.ACCESS_TOKEN, accessToken);

        CompletableFuture.runAsync(() -> {
            Entity<OutboundRequest> entity = Entity.entity(outboundRequest, MediaType.APPLICATION_JSON);
            Response response = target.request(MediaType.APPLICATION_JSON).post(entity);

            log.info("Message sent to: {}", outboundRequest.getRecipient().getId());
        }).exceptionally(e -> {
            log.error("Unexpected error when sending a message '{}:{}' when sending a message",
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

    public void sendQuickReply(String recipientId, String text, List<QuickReply> quickReplies) {
        OutboundRequest outboundRequest = OutboundRequest.builder()
            .recipient(new User(recipientId))
            .message(OutboundMessage.builder().text(text).quickReplies(quickReplies).build())
            .build();

        sendMessage(outboundRequest);
    }

    public void sendErrorMessage(String recipientId, String errorMessage) {
        sendTextMessage(recipientId, errorMessage);
    }
}
