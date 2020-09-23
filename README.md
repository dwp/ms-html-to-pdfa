# ms-html-to-pdfa
[![Build Status](https://travis-ci.org/dwp/ms-html-to-pdfa.svg?branch=master)](https://travis-ci.org/dwp/ms-html-to-pdfa) [![Known Vulnerabilities](https://snyk.io/test/github/dwp/ms-html-to-pdfa/badge.svg)](https://snyk.io/test/github/dwp/ms-html-to-pdfa)

RESTful service receiving json to construct a PDF document to various conformance levels

## build & run

Standard maven build.
* to package the `jar` file `mvn clean package`
* to run the application execute _jar -jar /path/to/jar/app.jar server /path/to/config.yml_
    * eg. `java -jar target/ms-html-to-pdfa-1.0-SNAPSHOT.jar server src/main/properties/dev.yml`

* from the IDE run the `uk.gov.dwp.pdfa.application.HtmlToPdfApplication` with program arguments `server path/to/properties.yml` (eg. _src/main/properties/dev.yml_)

NOTE: this application accepts environment variables that will be picked up at runtime (this file is bundled into to container).  If https configuration is needed a modified `config.yml` must be mounted into the container with the appropriate keystore/truststore locations (*see dropwizard documentation*).  

    server:
      applicationConnectors:
      - type: ${SERVER_APP_CONNECTOR:-http}
        port: ${SERVER_APP_PORT:-6677}
      adminConnectors:
      - type: ${SERVER_ADMIN_CONNECTOR:-http}
        port: ${SERVER_ADMIN_PORT:-0}
      requestLog:
        type: ${SERVER_REQUEST_LOG_TYPE:-external}

## `/generatePdf`
POST endpoint receiving the information to build the pdf file

    {
        "colour_profile": "base64-encoded-file",
        "font_map": {
            "tahoma": "base64-encoded-file",
            "arial": "base64-encoded-file"
        },
        "page_html": "base64-encoded-html",
        "conformance_level": "PDFA_1_A"
    }

* `colour_profile` (optional) : The base64 encoded colour profile file contents to be embedded to the pdf.  If this value is omitted or null the default colour profile will be applied (_src/main/resources/colours/sRBG.icm_)
* `font_map` (optional): a list of fonts to be embedded into the pdf.  If the `font_map` is missing or null then a 2 default fonts will be embedded into the document.
  * `arial` to cover basic fonts and `courier` to cover monospace requirements.
  * The format for each key/value item is:-
    * the name of the font (eg. arial), this must be specified in the html style header using the same format
    * the base64 encoded version of the `.ttf` file contents to be embedded with the file
* `page_html` (mandatory): The base64 encoded html document
* `conformance_level` (optional): The conformance level for the resulting pdf.  
If this parameter is missing (or null) it will default to PDFA_UA; the tightest of all the conformance levels.  

Pdf conformance levels are detailed [here](https://en.wikipedia.org/wiki/PDF/A#Conformance_levels_and_versions) with acceptable values for this service as:-

* `PDF_UA` (https://en.wikipedia.org/wiki/PDF/UA)
* `PDFA_1_A`
* `PDFA_1_B`
* `PDFA_2_A`
* `PDFA_2_B`
* `PDFA_3_A`
* `PDFA_3_B`
* `PDFA_3_U`
* `NONE`
    
The only **mandatory** parameter is the base64 encoded html.  If only the html is passed a standard colour profile will be used, `arial` (standard) and `courier` (monospace) will be embedded to the pdf and the conformance level for the pdf will be PDF/UA

Returns:-

* **200** :: Success.  Returns base64 encoded pdf in the response body
* **400** :: Bad or Malformed json document or json elements.  Returns a brief error message as the response body (full error is logged)
* **500** :: Internal error occurred, bad html or conformance levels, font/colour profile embedding.  Returns a brief error message as the response body (full error is logged)

#### Usage notes

For the incoming html there are 2 things to consider.  

* The pdf generator requires **XHTML** which requires careful closing of tags (https://www.w3schools.com/html/html_xhtml.asp)
* In order to satisfy the font requirements of PDFA_1_A document all elements need to reference the font that will be embedded.  This is best achieved by adding a `<STYLE>` element to the `<HEAD>` of the html and to apply it for all items (eg body).  The important point is to make sure that all fonts are explicitly specified in the html document.
* If using images it is best to encode the images directly into the html.  eg `<img src="data:image/png;base64,<the-base64-encoded-string-of-the-image>"/>`
* If using images `image-rendering` should be set pixelated or the following error will occur when trying to make any conformance level above NONE :-
    * https://github.com/veraPDF/veraPDF-validation-profiles/wiki/PDFA-Part-1-rules#rule-624-3
    * *"If an Image dictionary contains the Interpolate key, its value shall be false"*

eg.

```html
<html>
    <head>
        <style>
            pre, code, var {
                font-family: 'courier', serif;
            }
            body {
                font-family: 'arial', serif;
            }
            img {
                image-rendering: pixelated;
            }
        </style>
    </head>
    <body>
        <h1>hello world</h1>
        <img
            width="250px" height="250px"
            src="data:image/png;base64,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"
            alt="base64 encoded embedded image"
        />
    </body>
</html>
```

#### Common faults

* _fonts not embedded correctly_ :: will result in an error reporting `Index: 0, Size: 0` or `Index 0 out-of-bounds for length 0` which, whilst not a very clear, is because the required font is not present in the embedded list array.  All html tags should have an attached font (both normal and monospaced)
* _links not fully qualified_ :: any references to css or images that have relative paths will fail.  A full, resolvable URL is required.
* _closing tags_ :: XHTML requires all tags to be terminated, this is easily missed.

## `/version-info`

Endpoint to return a standard JSON document with build information.

* name: the `project.artifactId`
* version: the `project.version`
* build: the jenkins build-number
* build_time: the `maven.build.timestamp`

example output is:-

```
{
  "app": {
    "name": "ms-html-to-pdfa",
    "version": "1.6.0",
    "build": "133",
    "build_time": "2019-09-09T09:58:17Z"
  }
}
```

## Examples

The following will base64 encode the html file contents, call the service, decode the response and write to file on *nix based operating systems

`curl -m 10 -X POST --data '{"page_html":"'$(cat src/test/resources/successfulHtml.html | base64)'"}' http://localhost:6677/generatePdf | base64 -D > test.pdf`

This example will return the current build information

`curl http://localhost:6677/version-info`

### Continuous Integration (CI) Pipeline

For general information about the CI pipeline on this repository please see documentation at: https://confluence.service.dwpcloud.uk/x/_65dCg

**Pipeline Invocation**

This CI Pipeline now replaces the Jenkins Build CI Process for the `ms-html-to-pdfa`.

Gitlab CI will automatically invoke a pipeline run when pushing to a feature branch (this can be prevented using `[skip ci]` in your commit message if not required).

When a feature branch is merged into `develop` it will automatically start a `develop` pipeline and build the required artifacts.

For production releases please see the release process documented at: https://confluence.service.dwpcloud.uk/pages/viewpage.action?spaceKey=DHWA&title=SRE
A production release requires a manual pipeline (to be invoked by an SRE) this is only a release function. 
Production credentials are required.

**localdev Usage**

There is no change to the usage of localdev. The gitlab CI Build process create artifacts using the same naming convention as the old (no longer utilised) Jenkins CI Build process.

Therefore please continue to use `branch-develop` or `branch-f-*` (depending on branch name) for proving any feature changes.

**Access**

While this repository is open internally for read, no one has write access to this repository by default.
To obtain access to this repository please contact #ask-health-platform within slack and a member will grant the appropriate level of access.
