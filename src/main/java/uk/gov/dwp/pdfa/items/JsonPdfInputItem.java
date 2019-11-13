package uk.gov.dwp.pdfa.items;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.dwp.pdf.exception.PdfaGeneratorException;

import java.util.Base64;
import java.util.HashMap;
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

  public Map<String, byte[]> getFontMap() throws PdfaGeneratorException {
    Map<String, byte[]> outputMap = null;
    try {

      if (fontMap != null) {
        outputMap = new HashMap<>();

        for (Map.Entry<String, String> item : fontMap.entrySet()) {
          outputMap.put(item.getKey(), Base64.getDecoder().decode(item.getValue()));
        }
      }

    } catch (IllegalArgumentException e) {
      throw new PdfaGeneratorException(
          String.format("'font_map' elements are malformed :: %s", e.getMessage()));
    }

    return outputMap;
  }

  public byte[] getColourProfile() throws PdfaGeneratorException {
    byte[] colour;
    try {
      colour = colourProfile != null ? Base64.getDecoder().decode(colourProfile) : null;

    } catch (IllegalArgumentException e) {
      throw new PdfaGeneratorException(
          String.format("'colour_profile' element is malformed :: %s", e.getMessage()));
    }

    return colour;
  }

  public String getHtmlDocument() throws PdfaGeneratorException {
    String html;
    try {
      html = htmlDocument != null ? new String(Base64.getDecoder().decode(htmlDocument)) : null;

    } catch (IllegalArgumentException e) {
      throw new PdfaGeneratorException(
          String.format("'page_html' element is malformed :: %s", e.getMessage()));
    }

    return html;
  }

  public String getConformanceLevel() {
    return this.conformanceLevel;
  }
}
