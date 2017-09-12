package manfredlift.facebook.rmndr.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.CallbackHandler;
import manfredlift.facebook.rmndr.RmndrConfiguration;
import manfredlift.facebook.rmndr.RmndrConstants;
import manfredlift.facebook.rmndr.api.Callback;
import manfredlift.facebook.rmndr.util.SignatureVerifier;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static jersey.repackaged.com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Path("/webhook")
@Produces(MediaType.APPLICATION_JSON)
public class WebhookResource {
    private final RmndrConfiguration config;
    private final ObjectMapper objectMapper;
    private final CallbackHandler callbackHandler;

    public WebhookResource(RmndrConfiguration config, JerseyEnvironment jersey) {
        this.config = config;
        this.callbackHandler = checkNotNull(jersey.getProperty(RmndrConstants.CALLBACK_HANDLER));
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
            callbackHandler.handleCallbackAsync(callback);

            /* Always acknowledge with response 200 instantly, if signature is valid. (even with invalid request body)
             * Otherwise, Facebook would keep retrying the same request. */
            return Response.ok().build();
        } else {
            log.warn("Payload verification failed.");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
}