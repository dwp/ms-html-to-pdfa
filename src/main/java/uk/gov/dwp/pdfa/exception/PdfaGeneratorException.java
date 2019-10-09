package uk.gov.dwp.pdfa.exception;

public class PdfaGeneratorException extends Exception {

  public PdfaGeneratorException(String message, Throwable cause) {
    super(message, cause);
  }

  public PdfaGeneratorException(String message) {
    super(message);
  }
}
