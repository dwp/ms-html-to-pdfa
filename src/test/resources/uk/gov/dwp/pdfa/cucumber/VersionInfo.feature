Feature: Test version information endpoint

  @VersionInfo
  Scenario: Hit the endpoint with a GET and receive the information
    When I hit the service url "http://localhost:6677/version-info" with a GET request
      Then I get an http response of 200
      And The response string is equal to the info.json contents

  @VersionInfo
  Scenario: Hit the endpoint with the wrong protocol
    When I hit the service url "http://localhost:6677/version-info" with a POST request
      Then I get an http response of 405

  @VersionInfo
  Scenario: Hit another endpoint
    When I hit the service url "http://localhost:6677/version-spinfo" with a POST request
    Then I get an http response of 404
