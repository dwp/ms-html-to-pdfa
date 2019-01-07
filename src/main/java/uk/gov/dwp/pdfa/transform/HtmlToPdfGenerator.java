package uk.gov.dwp.pdfa.transform;

import com.openhtmltopdf.pdfboxout.PdfBoxFontResolver;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dwp.pdfa.exception.PdfaGeneratorException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

public class HtmlToPdfGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlToPdfGenerator.class.getName());

    public String createPdfaDocument(String html, String colourProfile, Map<String, String> fontMap, PdfRendererBuilder.PdfAConformance conformanceLevel) throws PdfaGeneratorException {
        try {
            String base64pdf;

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                PdfBoxRenderer pdfBuilder = new PdfRendererBuilder()
                        .defaultTextDirection(PdfRendererBuilder.TextDirection.LTR)
                        .useColorProfile(getColourProfile(colourProfile))
                        .withHtmlContent(getHtml(html), null)
                        .usePdfAConformance(conformanceLevel)
                        .toStream(outputStream)
                        .buildPdfRenderer();

                populateFontResolver(pdfBuilder.getFontResolver(), fontMap);
                pdfBuilder.createPDF();

                base64pdf = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                LOGGER.info("successfully generated base64 encoded pdf to conformance level {}", conformanceLevel);
            }

            return base64pdf;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.debug(e.getClass().getName(), e);
            throw new PdfaGeneratorException(e.getMessage(), e);
        }
    }

    private String getHtml(String html) {
        return html != null ? new String(Base64.getDecoder().decode(html.getBytes())) : null;
    }

    private byte[] getColourProfile(String base64ColourProfile) {
        return base64ColourProfile != null ? Base64.getDecoder().decode(base64ColourProfile.getBytes()) : null;
    }

    private void populateFontResolver(PdfBoxFontResolver fontResolver, Map<String, String> fontMap) {
        for (Map.Entry<String, String> entry : fontMap.entrySet()) {
            fontResolver.addFont(() -> new ByteArrayInputStream(Base64.getDecoder().decode(entry.getValue().getBytes())), entry.getKey(), null, null, false);
            LOGGER.debug("adding font '{}' to font map", entry.getKey());
        }
    }
}
