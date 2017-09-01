package manfredlift.facebook.rmndr.client;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.ReferenceTime;
import manfredlift.facebook.rmndr.api.WitResponse;
import org.glassfish.jersey.uri.UriComponent;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WitClient {

    private final String witToken;
    private final Client client;
    private final Gson gson;

    public WitClient(RmndrConfiguration configuration, Client client) {
        this.witToken = configuration.getWitToken();
        this.client = client;
        this.gson = new Gson();
    }

    public CompletableFuture<WitResponse> getResponseFuture(String query, ReferenceTime referenceTime) {
        WebTarget target = client.target(RmndrConstants.WIT_URI)
            .queryParam("q", query)
            .queryParam("context", UriComponent.encode(gson.toJson(referenceTime), UriComponent.Type.QUERY_PARAM));

        return CompletableFuture.supplyAsync(() -> {
            log.info("Sending request to Wit AI: '{}:{}'", query, referenceTime);
            WitResponse response = target.request().header(HttpHeaders.AUTHORIZATION, witToken).get(WitResponse.class);
            log.info("Response received from Wit AI");
            return response;
        });
    }
}
