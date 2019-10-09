package uk.gov.dwp.pdfa;

import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;

public class VersionInformationResourceTest {

  @Test
  public void verifyCorrectEntry() throws IOException {
    VersionInformationResource instance = new VersionInformationResource();
    JsonNode tree = new ObjectMapper().readTree(instance.getVersionInformation().getEntity().toString());

    assertNotNull(tree.get("app"));
    assertNotNull(tree.get("app").get("name"));
    assertNotNull(tree.get("app").get("version"));
    assertNotNull(tree.get("app").get("build"));
    assertNotNull(tree.get("app").get("build_time"));
  }
}
