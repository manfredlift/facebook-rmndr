package manfredlift.facebook.rmndr.client;

import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.ReferenceTime;
import manfredlift.facebook.rmndr.api.WitResponse;
import manfredlift.facebook.rmndr.util.DateHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WitClientTest {
    private WitClient witClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Client client;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebTarget target;

    @Before
    public void setup() {
        RmndrConfiguration config = new RmndrConfiguration();
        config.setWitToken("some_wit_token");

        witClient = new WitClient(config, client);
    }

    @Test
    public void getResponseTest() throws ExecutionException, InterruptedException {
        ReferenceTime referenceTime = DateHelper.referenceTimeFromMillis(1505035291868L, 3);
        String encodedReferenceTime = "%7B%22reference_time%22%3A%222017-09-10T12%3A21%3A31.868%2B03%3A00%22%7D";

        when(client.target(RmndrConstants.WIT_URI).queryParam("q", "some_query").queryParam(eq("context"), anyString()))
            .thenReturn(target);

        WitResponse witResponse = WitResponse.builder().msgId("some_message_id").build();
        when(target.request().header(HttpHeaders.AUTHORIZATION, "some_wit_token").get(WitResponse.class))
            .thenReturn(witResponse);

        WitResponse returnedWitResponse = witClient.getResponseFuture("some_query", referenceTime).get();
        assertEquals(witResponse, returnedWitResponse);


        verify(client.target(RmndrConstants.WIT_URI).queryParam("q", "some_query"))
            .queryParam("context", encodedReferenceTime);
        verify(target.request().header(HttpHeaders.AUTHORIZATION, "some_wit_token")).get(WitResponse.class);
    }


}
