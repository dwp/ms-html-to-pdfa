package uk.gov.dwp.pdfa;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/version-info")
public class VersionInformationResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(
          VersionInformationResource.class.getName()
  );

  @GET
  public Response getVersionInformation() {
    Response response;
    try {

      String output = IOUtils.toString(getClass().getResourceAsStream("/public/info.json"));
      LOGGER.debug("returning application build info");
      response = Response.ok().entity(output).build();

    } catch (IOException e) {
      response =
          Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      LOGGER.debug(e.getClass().getName(), e);
      LOGGER.error(e.getMessage());
    }

    return response;
  }
}
