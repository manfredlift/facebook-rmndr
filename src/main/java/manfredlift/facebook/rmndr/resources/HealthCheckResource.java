package manfredlift.facebook.rmndr.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/healthcheck")
public class HealthCheckResource {
    @GET
    public Response returnOk() {
        return Response.ok().build();
    }
}
