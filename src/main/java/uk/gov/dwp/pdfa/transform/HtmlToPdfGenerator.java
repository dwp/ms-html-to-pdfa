package uk.gov.dwp.pdfa.transform;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.TextDirection;
import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.pdfa.exception.PdfaGeneratorException;
import uk.gov.dwp.pdfa.items.PdfExtendedConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

public class HtmlToPdfGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlToPdfGenerator.class.getName());

  public String createPdfaDocument(
      String html, String colourProfile, Map<String, String> fontMap, String conformanceLevel)
      throws PdfaGeneratorException {
    try {
      String base64pdf;

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

        PdfRendererBuilder pdfBuilder =
            new PdfRendererBuilder()
                .defaultTextDirection(TextDirection.LTR)
                .useColorProfile(getColourProfile(colourProfile))
                .withHtmlContent(getHtml(html), null)
                .useFastMode()
                .toStream(outputStream);

        if (useConformanceLevelParameter(conformanceLevel)) {
          LOGGER.info("building pdf to comply with conformance level {}", conformanceLevel);
          pdfBuilder.usePdfAConformance(
              PdfRendererBuilder.PdfAConformance.valueOf(conformanceLevel));

        } else {
          LOGGER.info("building a PDF/UA accessible pdf");
          pdfBuilder.usePdfUaAccessbility(true);
        }

        verifyFontApplication(fontMap, getHtml(html), conformanceLevel);

        PdfBoxRenderer pdfBoxRenderer = pdfBuilder.buildPdfRenderer();
        populateFontResolver(pdfBoxRenderer.getFontResolver(), fontMap);
        pdfBoxRenderer.createPDF();

        base64pdf = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        LOGGER.info("successfully generated base64 encoded pdf");
      }

      return base64pdf;

    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      LOGGER.debug(e.getClass().getName(), e);
      throw new PdfaGeneratorException(e.getMessage(), e);
    }
  }

  private boolean useConformanceLevelParameter(String conformanceLevel)
      throws PdfaGeneratorException {
    boolean useConformanceLevel = false;

    try {
      PdfRendererBuilder.PdfAConformance.valueOf(conformanceLevel);
      useConformanceLevel = true;

    } catch (Exception e) {
      if ((conformanceLevel == null)
          || (!conformanceLevel.equalsIgnoreCase(PdfExtendedConstants.PDF_UA_CONFORMANCE))) {
        throw new PdfaGeneratorException(
            String.format("'%s' is not a valid conformance level, aborting", conformanceLevel));
      }
    }

    return useConformanceLevel;
  }

  private void verifyFontApplication(
      Map<String, String> fontMap, String html, String conformanceLevel)
      throws PdfaGeneratorException {
    if ((html != null)
        && (!conformanceLevel.equalsIgnoreCase(
            PdfRendererBuilder.PdfAConformance.NONE.toString()))) {
      LOGGER.debug("validate that all fonts in the document are contained in the font map");

      for (String fontHtml :
          html.lines().filter(p -> p.contains("font-family")).collect(Collectors.toList())) {
        boolean fontMissing = true;
        fontHtml = fontHtml.trim();

        for (String item : fontMap.keySet()) {
          if (fontHtml.contains(item)) {
            LOGGER.debug("successfully found embedded font {}'", item);
            fontMissing = false;
            break;
          }
        }

        if (fontMissing) {
          throw new PdfaGeneratorException(
              String.format(
                  "html element requests %s.  It is not passed in the font map, cannot encode.",
                  fontHtml.replace(";", "").trim()));
        }
      }
    }
  }

  private String getHtml(String html) {
    return html != null ? new String(Base64.getDecoder().decode(html.getBytes())) : null;
  }

  private byte[] getColourProfile(String base64ColourProfile) {
    return base64ColourProfile != null
        ? Base64.getDecoder().decode(base64ColourProfile.getBytes())
        : null;
  }

  private void populateFontResolver(PdfBoxFontResolver fontResolver, Map<String, String> fontMap) {
    for (Map.Entry<String, String> entry : fontMap.entrySet()) {
      fontResolver.addFont(
          () -> new ByteArrayInputStream(Base64.getDecoder().decode(entry.getValue().getBytes())),
          entry.getKey(),
          null,
          null,
          false);
      LOGGER.debug("adding font '{}' to font map", entry.getKey());
    }
  }
}
