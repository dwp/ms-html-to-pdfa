package uk.gov.dwp.pdfa.items;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class JsonPdfInputItem {

  @JsonProperty("font_map")
  private Map<String, String> fontMap;

  @JsonProperty("colour_profile")
  private String colourProfile;

  @JsonProperty("page_html")
  private String htmlDocument;

  @JsonProperty("conformance_level")
  private String conformanceLevel;

  public Map<String, String> getFontMap() {
    return fontMap;
  }

  public String getColourProfile() {
    return colourProfile;
  }

  public String getHtmlDocument() {
    return htmlDocument;
  }

  public String getConformanceLevel() {
    return this.conformanceLevel;
  }
}
