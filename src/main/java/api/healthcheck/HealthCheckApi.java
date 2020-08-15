package api.healthcheck;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v2")
public interface HealthCheckApi {

    @GET
    @Path("/healthcheck")
    @Produces(MediaType.TEXT_PLAIN)
    String checkHealth();
}
