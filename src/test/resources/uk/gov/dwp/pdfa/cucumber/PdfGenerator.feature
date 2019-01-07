Feature: Test pdf(a) generation from html

  @PdfGenerate
  Scenario: Given only HTML in the payload an A1A pdf is generated
    When I post to the service url "http://localhost:6677/generatePdf" with json constructed from the following source data
      | page_html      | src/test/resources/successfulHtml.html           |
    Then I get an http response of 200
    And the pdf is Base64 encoded and of the type PDFA_1_A

  @PdfGenerate
  Scenario: Given all values in the payload a PDFA_3_B pdf is generated
    When I post to the service url "http://localhost:6677/generatePdf" with json constructed from the following source data
      | page_html      | src/test/resources/successfulHtml.html           |
      | colour_profile | src/main/resources/colours/sRGB.icm              |
      | font_map       | {"arial": "src/main/resources/fonts/arialbd.ttf", "courier": "src/main/resources/fonts/courier.ttf"}|
      | conformance_level | PDFA_3_B                                       |
    Then I get an http response of 200
    And the pdf is Base64 encoded and of the type PDFA_3_B

  @PdfGenerate
  Scenario: Given overriding the font map without monospace it fails to embed
    When I post to the service url "http://localhost:6677/generatePdf" with json constructed from the following source data
      | page_html      | src/test/resources/successfulHtml.html           |
      | colour_profile | src/main/resources/colours/sRGB.icm              |
      | font_map       | {"arial": "src/main/resources/fonts/arial.ttf"}  |
      | conformance_level | PDFA_3_B                                       |
    Then I get an http response of 500

  @PdfGenerate
  Scenario: Given bad json i should get an error
    When I post to the service url "http://localhost:6677/generatePdf" with bad json
    Then I get an http response of 400

  @PdfGenerate
  Scenario: Given bad HTML i should get an error
    When I post to the service url "http://localhost:6677/generatePdf" with json constructed from the following source data
      | page_html      | src/test/resources/badHtmlFile.html           |
    Then I get an http response of 500

  @PdfGenerate @CH
  Scenario: Given bad font embedding I should get an error
    When I post to the service url "http://localhost:6677/generatePdf" with json constructed from the following source data
      | page_html      | src/test/resources/noFontSupplied.html           |
    Then I get an http response of 500

