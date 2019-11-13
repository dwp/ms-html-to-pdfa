package uk.gov.dwp.pdfa.items;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.dwp.pdf.exception.PdfaGeneratorException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JsonPdfInputItemTest {
  private static final String FULL_JSON =
      "{\"font_map\": {\"tahoma\":\"YmFzZTY0LWZvbnQ=\",\"arial\":\"YmFzZTY0LWZvbnQ=\"}, \"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\", \"conformance_level\": \"PDFA_1_A\"}";
  private static final String COLOUR_AND_HTML =
      "{\"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\"}";
  private static final String HTML_ONLY = "{\"page_html\": \"YmFzZTY0LWh0bWw=\"}";

  private static final String BAD_FONT_PLUS_COLOUR_HTML =
      "{\"font_map\": {\"tahoma\":\"i-am-not-base64\",\"arial\":\"YmFzZTY0LWZvbnQ=\"}, \"colour_profile\": \"YmFzZTY0LWNvbG91cg==\", \"page_html\": \"YmFzZTY0LWh0bWw=\"}";
  private static final String BAD_COLOUR_PLUS_HTML =
      "{\"colour_profile\": \"i-am-not-base64\", \"page_html\": \"YmFzZTY0LWh0bWw=\"}";
  private static final String BAD_HTML_ONLY = "{\"page_html\": \"i-am-not-base64\"}";

  @Test
  public void testFullSerialisationIsSuccessful() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem instance = new ObjectMapper().readValue(FULL_JSON, JsonPdfInputItem.class);

    assertNotNull(instance.getFontMap());
    assertNotNull(instance.getColourProfile());
    assertNotNull(instance.getHtmlDocument());
    assertThat(instance.getConformanceLevel(), is(equalTo("PDFA_1_A")));

    assertThat(instance.getFontMap().size(), is(equalTo(2)));
  }

  @Test
  public void testMissingFontIsSuccessful() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem instance =
        new ObjectMapper().readValue(COLOUR_AND_HTML, JsonPdfInputItem.class);

    assertNull(instance.getFontMap());
    assertNotNull(instance.getColourProfile());
    assertNotNull(instance.getHtmlDocument());
    assertNull(instance.getConformanceLevel());
  }

  @Test
  public void testMissingFontAndColourIsSuccessful() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem instance = new ObjectMapper().readValue(HTML_ONLY, JsonPdfInputItem.class);

    assertNull(instance.getFontMap());
    assertNull(instance.getColourProfile());
    assertNotNull(instance.getHtmlDocument());
    assertNull(instance.getConformanceLevel());
  }

  @Test
  public void testNonBase64FontIsInvalid() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem instance =
        new ObjectMapper().readValue(BAD_FONT_PLUS_COLOUR_HTML, JsonPdfInputItem.class);

    assertNotNull(instance.getColourProfile());
    assertNull(instance.getConformanceLevel());
    assertNotNull(instance.getHtmlDocument());

    try {
      assertNotNull(instance.getFontMap());
      fail("should throw error");

    } catch (PdfaGeneratorException e) {
      assertThat(e.getMessage(), startsWith("'font_map'"));
    }
  }

  @Test
  public void testNonBase64ColourIsInvalid() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem instance =
        new ObjectMapper().readValue(BAD_COLOUR_PLUS_HTML, JsonPdfInputItem.class);

    assertNull(instance.getFontMap());
    assertNull(instance.getConformanceLevel());
    assertNotNull(instance.getHtmlDocument());

    try {
      assertNotNull(instance.getColourProfile());
      fail("should throw error");

    } catch (PdfaGeneratorException e) {
      assertThat(e.getMessage(), startsWith("'colour_profile'"));
    }
  }

  @Test
  public void testNonBase64HtmlIsInvalid() throws IOException, PdfaGeneratorException {
    JsonPdfInputItem instance = new ObjectMapper().readValue(BAD_HTML_ONLY, JsonPdfInputItem.class);

    assertNull(instance.getFontMap());
    assertNull(instance.getColourProfile());
    assertNull(instance.getConformanceLevel());

    try {
      assertNotNull(instance.getHtmlDocument());
      fail("should throw error");

    } catch (PdfaGeneratorException e) {
      assertThat(e.getMessage(), startsWith("'page_html'"));
    }
  }
}
