package manfredlift.facebook.rmndr.resources;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import manfredlift.facebook.rmndr.RmndrApplication;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class WebhookResourceTest {
    @ClassRule
    public static final DropwizardAppRule<RmndrConfiguration> app =
        new DropwizardAppRule<>(RmndrApplication.class,
            ResourceHelpers.resourceFilePath("test-config.yml"));

    private Client client;
    private String hostUrl;

    @Before
    public void setup() {
        client = app.client();
        hostUrl = "http://localhost:8089/webhook";
    }

    @Test
    public void verifyWebhookTest_success() {
        Response response = client.target(hostUrl)
            .queryParam("hub.mode", "mode")
            .queryParam("hub.verify_token", "test_verify_token")
            .queryParam("hub.challenge", "test_challenge")
            .request()
            .get();

        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(response.readEntity(String.class), equalTo("test_challenge"));
    }

    @Test
    public void verifyWebhookTest_fail() {
        Response response = client.target(hostUrl)
            .queryParam("hub.mode", "mode")
            .queryParam("hub.verify_token", "wrong_verify_token")
            .queryParam("hub.challenge", "test_challenge")
            .request()
            .get();

        assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }
}
